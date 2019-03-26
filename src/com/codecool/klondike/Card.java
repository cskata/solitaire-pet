package com.codecool.klondike;

import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.util.*;

public class Card extends ImageView {
    enum suits {HEARTS, DIAMONDS, SPADES, CLUBS}

    enum ranks {ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING}

    private List<Pile> movements = new ArrayList<>();
    private int suit;
    private int rank;
    private boolean faceDown;
    private String color;


    private Image backFace;
    private Image frontFace;
    private Pile containingPile;


    public void setFinalDestPile(Pile finalDestPile) {
        this.finalDestPile = finalDestPile;
    }

    public Pile getFinalDestPile() {
        return finalDestPile;
    }
    private Pile finalDestPile;
    private DropShadow dropShadow;

    static Image cardBackImage;
    private static final Map<String, Image> cardFaceImages = new HashMap<>();
    public static final int WIDTH = 150;
    public static final int HEIGHT = 215;


    public Pile getLastMovement() {
        return this.movements.get(this.movements.size() - 1);
    }

    public void addMovement(Pile pile) {
        this.movements.add(pile);
        System.out.println(this.movements.toString());
    }

    public void undoLastCardMovement() {
        this.movements.remove(this.movements.size() - 1);
    }

    public Card(int suit, int rank, boolean faceDown) {
        this.suit = suit;

        if (suit == 1 || suit == 2) {
            this.color = "red";
        } else {
            this.color = "black";
        }

        this.rank = rank;
        this.faceDown = faceDown;
        this.dropShadow = new DropShadow(2, Color.gray(0, 0.75));
        backFace = cardBackImage;
        frontFace = cardFaceImages.get(getShortName());
        setImage(faceDown ? backFace : frontFace);
        setEffect(dropShadow);
    }

    public String getColor() {
        return color;
    }


    public int getSuit() {
        return suit;
    }

    public int getRank() {
        return rank;
    }

    public boolean isFaceDown() {
        return faceDown;
    }

    public String getShortName() {
        return "S" + suit + "R" + rank;
    }

    public DropShadow getDropShadow() {
        return dropShadow;
    }

    public Pile getContainingPile() {
        return containingPile;
    }

    public void setContainingPile(Pile containingPile) {
        this.containingPile = containingPile;
    }

    public void setBackFace(Image backFace) {
        this.backFace = backFace;
    }

    public void moveToPile(Pile destPile) {
        this.getContainingPile().getCards().remove(this);
        destPile.addCard(this);
    }

    public void flip() {
        faceDown = !faceDown;
        setImage(faceDown ? backFace : frontFace);
    }

    @Override
    public String toString() {
        return "The " + "Rank" + rank + " of " + "Suit" + suit;
    }

    public static boolean isOppositeColor(Card card1, Card card2) {
        return card1.getColor().equals(card2.getColor());
    }

    public static boolean isSameSuit(Card card1, Card card2) {
        return card1.getSuit() == card2.getSuit();
    }

    public static List<Card> createNewDeck() {
        List<Card> result = new ArrayList<>();
        for (suits suit : suits.values()) {
            int currentSuit = (suits.valueOf(suit.toString()).ordinal() + 1);
            for (ranks rank : ranks.values()) {
                int currentRank = (ranks.valueOf(rank.toString()).ordinal() + 1);
                result.add(new Card(currentSuit, currentRank, true));
            }
        }
        return result;
    }

    public static void loadCardImages() {
        cardBackImage = new Image("card_images/card_back_green.png");
        String suitName = "";
        int suitIndex;

        for (suits suit : suits.values()) {
            suitName = suit.toString().toLowerCase();
            suitIndex = (suits.valueOf(suit.toString()).ordinal() + 1);

            for (ranks rank : ranks.values()) {
                int currentRank = (ranks.valueOf(rank.toString()).ordinal() + 1);
                String cardName = suitName + currentRank;
                String cardId = "S" + suitIndex + "R" + currentRank;
                String imageFileName = "card_images/" + cardName + ".png";
                cardFaceImages.put(cardId, new Image(imageFileName));
            }
        }
    }
}
