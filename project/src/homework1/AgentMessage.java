/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework1;

import java.io.Serializable;

/**
 *
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class AgentMessage implements Serializable {
    String type;
    Object content;

    public AgentMessage(String type, Object content) {
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public Object getContent() {
        return content;
    }
}
