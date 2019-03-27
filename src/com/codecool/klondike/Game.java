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
    private List<Card> deckListForReference = FXCollections.observableArrayList();
    private List<Card> remainingCardsInTableau = FXCollections.observableArrayList();

    private static ObservableList<String> movesDuringGame = FXCollections.observableArrayList();
    private static ObservableList<Card> movedCardsDuringGame = FXCollections.observableArrayList();
    private static ObservableList<ObservableList<Card>> movedCardListsDuringGame = FXCollections.observableArrayList();

    private Pile stockPile;
    private Pile discardPile;
    private ObservableList<Pile> foundationPiles = FXCollections.observableArrayList();
    private ObservableList<Pile> tableauPiles = FXCollections.observableArrayList();

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
                movesDuringGame.add("Card");
                movedCardsDuringGame.add(card);
                card.addMovement(clickedPile);

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
                    removeCardAndFlipNext(card, destPile);
                }
            } else {
                if (card.getRank() == 1) {
                    removeCardAndFlipNext(card, destPile);
                    break;
                }
            }
        }
    }

    private void removeCardAndFlipNext(Card card, Pile destPile) {
        draggedCards.add(card);
        Pile clickedPile = card.getContainingPile();
        movesDuringGame.add("Card");
        movedCardsDuringGame.add(card);
        doCardRelocation(destPile, clickedPile, card);
        MouseUtil.slideToDest(draggedCards, destPile);

        if (!clickedPile.isEmpty()
                && clickedPile.getPileType().equals(Pile.PileType.TABLEAU)
                && clickedPile.getTopCard().isFaceDown()) {
            clickedPile.getTopCard().flip();
            addMouseEventHandlers(clickedPile.getTopCard());
        }
        checkEndGame();
        draggedCards.clear();
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
    };

    public void checkEndGame() {
        if (isGameWon()) {
            for (Card card : deckListForReference) {
                if (card.getContainingPile().getPileType().equals(Pile.PileType.TABLEAU)) {
                    for (Pile pile : foundationPiles) {
                        if (Card.isSameSuit(card, pile.getTopCard())) {
                            card.setFinalDestPile(foundationPiles.get(foundationPiles.indexOf(pile)));
                        }
                    }
                    remainingCardsInTableau.add(card);
                }
            }

            for (Card card : remainingCardsInTableau) {
                MouseUtil.autoSlideCard(card, card.getFinalDestPile());
            }

            removeMouseEventHandlers();
            alertWin();
        }
    }

    private void alertWin() {
        Alert alert = new Alert(Alert.AlertType.NONE, "Do you want to play again?",
                ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        alert.setTitle("You won!");
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
        int cardsInFoundation = 0;

        for (Card card : deckListForReference) {
            if (!card.isFaceDown() && !card.getContainingPile().getPileType().equals(Pile.PileType.DISCARD)) {
                cardsInFoundation++;
            }
        }

        return cardsInFoundation == 52;
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
                card.addMovement(discardPile);
            }
            discardPile.clear();
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

        if (sourcePile.getPileType() == Pile.PileType.TABLEAU && draggedCards.size() > 1) {
            movesDuringGame.add("List");
            ObservableList<Card> toAdd = FXCollections.observableArrayList();
            toAdd.addAll(draggedCards);
            movedCardListsDuringGame.add(movedCardListsDuringGame.size(), toAdd);

        } else {
            movesDuringGame.add("Card");
            movedCardsDuringGame.add(draggedCards.get(0));
        }
        for (Card card : cardsToAdd) {
            doCardRelocation(destPile, sourcePile, card);
        }
        MouseUtil.slideToDest(draggedCards, destPile);
    }

    private void doCardRelocation(Pile destPile, Pile sourcePile, Card card) {
        card.addMovement(sourcePile);
        card.setContainingPile(destPile);
        sourcePile.removeCard(card);
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

        addUndoButton();
    }

    private void addUndoButton() {
        Button undoButton = new Button("Undo");
        undoButton.setLayoutX(460);
        undoButton.setLayoutY(50);

        undoButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (movesDuringGame.size() > 0) {
                    handleUndo();
                }
            }
        });

        getChildren().add(undoButton);
    }

    private void handleUndo() {
        String lastMovement = movesDuringGame.get(movesDuringGame.size() - 1);

        if (lastMovement.equals("Card")) {
            Card lastMovedCard = movedCardsDuringGame.get(movedCardsDuringGame.size() - 1);
            Pile currentPile = lastMovedCard.getContainingPile();
            Pile previousPile = lastMovedCard.getLastMovement();

            lastMovedCard.undoLastMovement();
            if (previousPile.getPileType().equals(Pile.PileType.STOCK)) {
                lastMovedCard.flip();
            } else if (!previousPile.isEmpty()
                    && previousPile.getPileType().equals(Pile.PileType.TABLEAU)
                    && !previousPile.getTopCard().isFaceDown()) {
                previousPile.getTopCard().flip();
            }

            currentPile.removeCard(lastMovedCard);
            previousPile.addCard(lastMovedCard);
            movedCardsDuringGame.remove(movedCardsDuringGame.size() - 1);
            movesDuringGame.remove(movesDuringGame.size() - 1);
        } else if (lastMovement.equals("List")) {
            List<Card> lastMovedCards = movedCardListsDuringGame.get(movedCardListsDuringGame.size() - 1);
            Pile currentPile = lastMovedCards.get(0).getContainingPile();
            Pile previousPile = lastMovedCards.get(0).getLastMovement();

            if (!previousPile.isEmpty() && !previousPile.getTopCard().isFaceDown()) {
                previousPile.getTopCard().flip();
            }

            for (Card card : lastMovedCards) {
                card.undoLastMovement();
                currentPile.removeCard(card);
                previousPile.addCard(card);
            }

            movedCardListsDuringGame.remove(movedCardListsDuringGame.size() - 1);
            movesDuringGame.remove(movesDuringGame.size() - 1);
        }
    }
}
