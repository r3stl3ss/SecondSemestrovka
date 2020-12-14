import java.awt.*;

public class PlayableWord {
    String word;
    Color currentColor;

    public PlayableWord(String word, Color currentColor) {
        this.word = word;
        this.currentColor = currentColor;
    }

    public String printWord(PlayableWord pw) {
        return pw.word;
    }
}
