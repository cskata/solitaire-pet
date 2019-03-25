package com.codecool.klondike;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();
    private List<Card> deckForReference = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();
    private List<Card> cardsToDrag = FXCollections.observableArrayList();

    private static double STOCK_GAP = 0;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile clickedPile = card.getContainingPile();

        if (clickedPile.getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        for (int i = activePile.getCards().indexOf(card); i < activePile.numOfCards(); i++) {
            cardsToDrag.add(activePile.getCards().get(i));
        }

        draggedCards.clear();
        draggedCards.addAll(cardsToDrag);
        cardsToDrag.clear();
        for (Card c : draggedCards) {
            c.getDropShadow().setRadius(20);
            c.getDropShadow().setOffsetX(10);
            c.getDropShadow().setOffsetY(10);

            c.toFront();
            c.setTranslateX(offsetX);
            c.setTranslateY(offsetY);
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty()) {
            return;
        }
        Card card = (Card) e.getSource();
        Pile pile;

        if (e.getSceneY() < 300.0 && e.getSceneX() > 600) {
            pile = getValidIntersectingPile(card, foundationPiles);
        } else {
            pile = getValidIntersectingPile(card, tableauPiles);
        }

        if (pile != null) {
            handleValidMove(card, pile);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
        }
        draggedCards.clear();
        alertWin();
    };

    public void alertWin() {
        if (isGameWon()) {
            for (Pile pile : foundationPiles) {
                for (Card card : pile.getCards()) {
                    card.setOnMouseClicked(null);
                    card.setOnMousePressed(null);
                    card.setOnMouseDragged(null);
                    card.setOnMouseReleased(null);
                }
            }
            Alert alert = new Alert(Alert.AlertType.NONE, "Do you want to play again?",
                    ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            alert.showAndWait();

            if (alert.getResult() == ButtonType.YES) {
                System.out.println("yayyy");
            } else if (alert.getResult() == ButtonType.NO) {
                System.out.println("okay bye");
                Platform.exit();
            }
        }
    }

    public boolean isGameWon() {
        int fullPiles = 0;
        int almostFullPile = 0;
        for (int i = 0; i < foundationPiles.size(); i++) {
            if (foundationPiles.get(i).numOfCards() == Card.ranks.values().length) {
                fullPiles++;
            } else if (foundationPiles.get(i).numOfCards() == 12) {
                almostFullPile++;
            }
        }
        return fullPiles == 3 && almostFullPile == 1;
    }

    Game() {
        deck = Card.createNewDeck();
        deckForReference.addAll(deck);
        setBackgroundButtons();
        initPiles();
        dealCards();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        if (stockPile.isEmpty()) {
            ObservableList<Card> discarded = discardPile.getCards();
            Collections.reverse(discarded);
            for (Card card : discarded) {
                card.flip();
                stockPile.addCard(card);
            }
            discardPile.clear();
            System.out.println("Stock refilled from discard pile.");
        }
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        if (destPile.getPileType().equals(Pile.PileType.TABLEAU)) {
            if (!destPile.isEmpty() && !destPile.getTopCard().isFaceDown()) {
                int clickedCardRank = card.getRank();
                int targetPileTopCardRank = destPile.getTopCard().getRank();
                String clickedCardColor = card.getColor();
                String targetPileTopCardColor = destPile.getTopCard().getColor();
                return clickedCardRank + 1 == targetPileTopCardRank && !clickedCardColor.equals(targetPileTopCardColor);
            } else {
                return card.getRank() == 13;
            }
        } else if (destPile.getPileType().equals(Pile.PileType.FOUNDATION)) {
            if (destPile.isEmpty() && card.getRank() == 1) {
                return true;
            } else if (!destPile.isEmpty()
                    && card.getRank() == destPile.getTopCard().getRank() + 1
                    && card.getSuit() == destPile.getTopCard().getSuit()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        Pile.PileType currentPileType = card.getContainingPile().getPileType();
        findOriginalPile(card, destPile, currentPileType);
    }

    private void findOriginalPile(Card card, Pile destPile, Pile.PileType currentPileType) {
        if (currentPileType.equals(Pile.PileType.DISCARD)) {
            relocateCard(destPile, discardPile);
        } else if (currentPileType.equals(Pile.PileType.TABLEAU)) {
            int origPileNum = tableauPiles.indexOf(card.getContainingPile());
            Pile sourcePile = tableauPiles.get(origPileNum);
            relocateCard(destPile, sourcePile);
            autoFlipNextCard(sourcePile);
        } else if (currentPileType.equals(Pile.PileType.FOUNDATION)) {
            int origPileNum = foundationPiles.indexOf(card.getContainingPile());
            Pile sourcePile = foundationPiles.get(origPileNum);
            relocateCard(destPile, sourcePile);
        }
    }

    /**
     * Automatically flips the top card in the affected tableau pile if the card is facing down
     */
    private void autoFlipNextCard(Pile sourcePile) {
        if (!sourcePile.isEmpty() && sourcePile.getTopCard().isFaceDown()) {
            Card nextCardInOrigPile = sourcePile.getTopCard();
            nextCardInOrigPile.flip();
            addMouseEventHandlers(nextCardInOrigPile);
        }
    }

    private void relocateCard(Pile destPile, Pile sourcePile) {
        List<Card> cardsToAdd = FXCollections.observableArrayList();
        cardsToAdd.addAll(draggedCards);
        Collections.reverse(cardsToAdd);
        for (Card card : cardsToAdd) {
            sourcePile.removeCard(card);
        }
        MouseUtil.slideToDest(draggedCards, destPile);
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(60);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(250);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(600 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(60 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        //add cards to tableau piles
        for (Pile tableau : tableauPiles) {
            int tableauIndex = tableauPiles.indexOf(tableau);
            int cardsToAdd = tableauIndex + 1;

            for (int i = 0; i < cardsToAdd; i++) {
                Card cardToAdd = deck.get(i);
                tableau.addCard(cardToAdd);
                cardToAdd.setOnMouseClicked(onMouseClickedHandler);
                getChildren().add(cardToAdd);
                deck.remove(cardToAdd);
            }
            tableau.getTopCard().flip();
            addMouseEventHandlers(tableau.getTopCard());
        }

        Iterator<Card> deckIterator = deck.iterator();
        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });
    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    public void setBackgroundButtons() {
        switchTheme("Blue", 60, "#0097e6");
        switchTheme("Purple", 140, "#7158e2");
        switchTheme("Red", 220, "#EA2027");
        switchTheme("Green", 300, "green");
    }

    public void switchTheme(String text, int layoutX, String color) {
        Button button = new Button(text);
        button.setLayoutX(layoutX);
        button.setLayoutY(640);
        button.setStyle("-fx-pref-height: 30.0; -fx-pref-width: 70.0; -fx-font-weight: bold");
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setStyle("-fx-background-color: " + color);
                switchCardBack();
            }
        });
        getChildren().add(button);
    }

    private void switchCardBack() {
        Image newImage = new Image("/card_images/card_back_red.png");
        for (Card card : deckForReference) {
            if (card.isFaceDown()) {
                card.setImage(newImage);
            }
            card.setBackFace(newImage);
        }
    }
}
