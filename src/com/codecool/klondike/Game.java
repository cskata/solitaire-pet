package com.codecool.klondike;

import com.sun.xml.internal.bind.v2.model.core.ID;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

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

        // flips the top card in every tableau and checks if the clicked card is the top card
        if (card.isFaceDown() && clickedPile.getTopCard() == card) {
            card.flip();
            // only flipped cards can have click events
            addMouseEventHandlers(card);
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

        draggedCards.clear();
        draggedCards.add(card);

        card.getDropShadow().setRadius(20);
        card.getDropShadow().setOffsetX(10);
        card.getDropShadow().setOffsetY(10);

        card.toFront();
        card.setTranslateX(offsetX);
        card.setTranslateY(offsetY);
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty()) {
            return;
        }
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, tableauPiles);

        if (pile != null) {
            handleValidMove(card, pile);
            draggedCards.clear();
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
    };

    public boolean isGameWon() {
        //TODO
        return false;
    }

    Game() {
        deck = Card.createNewDeck();
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
        if (!destPile.isEmpty()) {
            int clickedCardRank = card.getRank();
            int targetPileTopCardRank = destPile.getTopCard().getRank();
            String clickedCardColor = card.getColor();
            String targetPileTopCardColor = destPile.getTopCard().getColor();
            return clickedCardRank + 1 == targetPileTopCardRank && !clickedCardColor.equals(targetPileTopCardColor);
        } else {
            return card.getRank() == 13;
        }

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
        boolean isCardTheKing = card.getRank() == 13;

        if (destPile.numOfCards() != 0 && isMoveValid(card, destPile)) {
            Pile.PileType currentPileType = card.getContainingPile().getPileType();
            if (currentPileType.equals(Pile.PileType.TABLEAU)) {
                int origPileNum = tableauPiles.indexOf(card.getContainingPile());
                relocateCardFromTableau(card, destPile, tableauPiles.get(origPileNum));
            } else if (card.getContainingPile().getPileType().equals(Pile.PileType.DISCARD)) {
                relocateCardFromTableau(card, destPile, discardPile);
            }

        } else if (destPile.isEmpty() && isCardTheKing) {
            int origPileNum = tableauPiles.indexOf(card.getContainingPile());
            relocateCardFromTableau(card, destPile, tableauPiles.get(origPileNum));
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
    }

    private void relocateCardFromTableau(Card card, Pile destPile, Pile pile) {
        destPile.addCard(card);
        pile.removeCard(card);
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
                //faceDown cards can only have click event to flip, that can do everything
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
        addBackgroundColorSwitcher("Blue", 60, "#0097e6");
        addBackgroundColorSwitcher("Purple", 140, "#7158e2");
        addBackgroundColorSwitcher("Red", 220, "#EA2027");
        addBackgroundColorSwitcher("Green", 300, "green");
    }

    public void addBackgroundColorSwitcher(String text, int layoutX, String color) {
        Button button = new Button(text);
        button.setLayoutX(layoutX);
        button.setLayoutY(640);
        button.setStyle("-fx-pref-height: 30.0; -fx-pref-width: 70.0; -fx-font-weight: bold");
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setStyle("-fx-background-color: " + color);
            }
        });
        getChildren().add(button);
    }
}
