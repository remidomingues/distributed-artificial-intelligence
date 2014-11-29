/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework3;

import java.io.Serializable;

/**
 *
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class AuctionResult implements Serializable {
    public int artifactID;
    public String winnerName;
    public double price;

    public AuctionResult(int artifactID, String winnerName, double price) {
        this.artifactID = artifactID;
        this.winnerName = winnerName;
        this.price = price;
    }
}
