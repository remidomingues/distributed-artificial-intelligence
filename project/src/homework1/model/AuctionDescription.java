/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework1.model;

/**
 * Auction description: price and artifact ID
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class AuctionDescription {
    private int artifactID;
    private double price;

    /**
     * Constructor
     * @param artifactID
     * @param price 
     */
    public AuctionDescription(int artifactID, double price) {
        this.artifactID = artifactID;
        this.price = price;
    }

    public int getArtifactID() {
        return artifactID;
    }

    public double getPrice() {
        return price;
    }
}
