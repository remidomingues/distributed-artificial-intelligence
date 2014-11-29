package nqueens;


import java.io.Serializable;

/**
 * Serializable message defined by a type and a content object
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class AgentMessage implements Serializable {
    String type;
    Object content;

    /**
     * Constructor
     * @param type
     * @param content 
     */
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
