import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class GenBanner {
    static final Color BG = new Color(0x1A1D24), FG = new Color(0xF1F1F1), MUTE = new Color(0x7E8591), ACC = new Color(0x6FB7A8);
    static Graphics2D g(BufferedImage im) {
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(BG);
        g.fillRect(0, 0, im.getWidth(), im.getHeight());
        return g;
    }
    static void grid(Graphics2D g, float x, float y, float s, float gap, float r) {
        Color[] c = {FG, MUTE, MUTE, ACC};
        for (int i = 0; i < 4; i++) {
            g.setColor(c[i]);
            g.fill(new RoundRectangle2D.Float(x + (i % 2) * (s + gap), y + (i / 2) * (s + gap), s, s, r, r));
        }
    }
    public static void main(String[] a) throws Exception {
        BufferedImage b = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = g(b);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        FontMetrics m = g.getFontMetrics();
        String t = "miniHome";
        float s = 54, gap = 12, gw = 2 * s + gap, space = 38, tw = m.stringWidth(t);
        float x0 = (640 - (gw + space + tw)) / 2f, cy = 180;
        grid(g, x0, cy - gw / 2f, s, gap, 14);
        g.setColor(FG);
        g.drawString(t, x0 + gw + space, cy + m.getAscent() / 2f - m.getDescent() / 2f - 4);
        ImageIO.write(b, "png", new File(a[0] + "/banner.png"));

        BufferedImage i = new BufferedImage(192, 192, BufferedImage.TYPE_INT_RGB);
        g = g(i);
        grid(g, 41, 41, 52, 16, 14);
        ImageIO.write(i, "png", new File(a[0] + "/icon.png"));
    }
}
