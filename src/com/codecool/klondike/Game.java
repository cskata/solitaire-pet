package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.*;

public class Game extends Pane {
    private Stage currentStage = Klondike.getPrimaryStage();

    private List<Card> deck;
    private List<Card> deckListForReference = new ArrayList<>();

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

    private Map<String, String> colors = new TreeMap<String, String>() {{
        put("Blue", "#0097e6");
        put("Green", "green");
        put("Purple", "#7158e2");
        put("Red", "#eb4d4b");
        put("Loops", "#81ecec");
    }};


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile clickedPile = card.getContainingPile();

        if (e.getClickCount() == 1) {
            if (clickedPile.getPileType() == Pile.PileType.STOCK) {
                card.moveToPile(discardPile);
                card.flip();
                card.setMouseTransparent(false);
            }
        }

        if (e.getClickCount() == 2) {
            handleDoubleClick(card);
        }
    };

    private void handleDoubleClick(Card card) {
        for (Pile destPile : foundationPiles) {
            Card topCard = destPile.getTopCard();
            if (!destPile.isEmpty()) {
                if (Card.isSameSuit(card, topCard) && topCard.getRank() + 1 == card.getRank()) {
                    draggedCards.add(card);
                    removeCardAndFlipNext(card, destPile);
                    draggedCards.clear();
                }
            } else {
                if (card.getRank() == 1) {
                    draggedCards.add(card);
                    removeCardAndFlipNext(card, destPile);
                    draggedCards.clear();
                    break;
                }
            }
        }
    }

    private void removeCardAndFlipNext(Card card, Pile destPile) {
        Pile clickedPile = card.getContainingPile();
        MouseUtil.slideToDest(draggedCards, destPile);
        clickedPile.removeCard(card);
        endGame();
        if (!clickedPile.isEmpty()
                && clickedPile.getPileType().equals(Pile.PileType.TABLEAU)
                && clickedPile.getTopCard().isFaceDown()) {
            clickedPile.getTopCard().flip();
            addMouseEventHandlers(clickedPile.getTopCard());
        }
    }

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

        for (Card draggedCard : draggedCards) {
            draggedCard.getDropShadow().setRadius(20);
            draggedCard.getDropShadow().setOffsetX(10);
            draggedCard.getDropShadow().setOffsetY(10);

            draggedCard.toFront();
            draggedCard.setTranslateX(offsetX);
            draggedCard.setTranslateY(offsetY);
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
        endGame();
    };

    public void endGame() {
        if (isGameWon()) {
            removeMouseEventHandlers();
            alertWin();
        }
    }

    private void alertWin() {
        Alert alert = new Alert(Alert.AlertType.NONE, "Do you want to play again?",
                ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            Scene scene = Klondike.startGame();
            currentStage.setScene(scene);
        } else if (alert.getResult() == ButtonType.NO) {
            currentStage.close();
        }
    }

    private void removeMouseEventHandlers() {
        for (Pile pile : foundationPiles) {
            for (Card card : pile.getCards()) {
                card.setOnMouseClicked(null);
                card.setOnMousePressed(null);
                card.setOnMouseDragged(null);
                card.setOnMouseReleased(null);
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
        createGameMenu();
        deck = Card.createNewDeck();
        deckListForReference.addAll(deck);
        Collections.shuffle(deck);

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
                return clickedCardRank + 1 == targetPileTopCardRank && !Card.isOppositeColor(card, destPile.getTopCard());
            } else {
                return card.getRank() == 13;
            }
        } else if (destPile.getPileType().equals(Pile.PileType.FOUNDATION)) {
            if (destPile.isEmpty() && card.getRank() == 1) {
                return true;
            } else if (!destPile.isEmpty()
                    && card.getRank() == destPile.getTopCard().getRank() + 1
                    && Card.isSameSuit(card, destPile.getTopCard())) {
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
            card.setContainingPile(destPile);
        }
        MouseUtil.slideToDest(draggedCards, destPile);
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(60);
        stockPile.setLayoutY(40);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(250);
        discardPile.setLayoutY(40);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(600 + i * 180);
            foundationPile.setLayoutY(40);
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

    private void switchCardBack(String color) {
        Image newImage = new Image("/card_images/card_back_" + color + ".png");
        for (Card card : deckListForReference) {
            if (card.isFaceDown()) {
                card.setImage(newImage);
            }
            card.setBackFace(newImage);
        }
    }

    private void createGameMenu() {
        Menu menuFile = new Menu("File");
        menuFile.setStyle("-fx-font-weight: bold");

        MenuItem menuNewGame = new MenuItem("New Game");
        menuNewGame.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Scene scene = Klondike.startGame();
                currentStage.setScene(scene);
            }
        });

        MenuItem menuExit = new MenuItem("Exit");
        menuExit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                currentStage.close();
            }
        });

        menuFile.getItems().add(menuNewGame);
        menuFile.getItems().add(menuExit);

        Menu menuTheme = new Menu("Choose a theme");
        menuTheme.setStyle("-fx-font-weight: bold");

        for (Map.Entry<String, String> color : colors.entrySet()) {
            MenuItem item = new MenuItem(color.getKey());
            if (!color.getKey().equals("Loops")) {
                item.setStyle("-fx-text-fill: " + color.getKey());
            }

            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    setStyle("-fx-background-color: " + color.getValue());
                    switchCardBack(color.getKey().toLowerCase());
                }
            });

            menuTheme.getItems().add(item);
        }

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().add(menuFile);
        menuBar.getMenus().add(menuTheme);

        menuBar.setStyle("-fx-pref-width: 1400");

        getChildren().add(menuBar);
    }
}
