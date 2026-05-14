package org.example.Client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.Server.ScoreBoard.PlayerScore;
import org.example.shared.Message;
import org.example.shared.Question;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.List;

/**
 * ClientGUI.java — Clean dark theme
 *
 * Screens:
 *   1. Waiting — shown while lobby fills
 *   2. Question — question text + 4 colored answer buttons + timer ring
 *   3. Scoreboard — ranked rows with score bars
 *   4. Game Over — final results
 */
public class ClientGUI {

    // ── Color palette ─────────────────────────────────────
    private static final Color BG       = new Color(13,  17,  23);
    private static final Color SURFACE  = new Color(22,  27,  34);
    private static final Color SURFACE2 = new Color(30,  37,  46);
    private static final Color ACCENT   = new Color(88,  166, 255);
    private static final Color TEXT_PRI = new Color(230, 237, 243);
    private static final Color TEXT_SEC = new Color(139, 148, 158);
    private static final Color BORDER   = new Color(48,  54,  61);

    private static final Color[] BTN_COLORS = {
        new Color(88,  166, 255),
        new Color(63,  185, 80),
        new Color(255, 123, 114),
        new Color(210, 153, 34),
    };

    // ── Fonts ─────────────────────────────────────────────
    private static final Font F_TITLE = new Font("SansSerif", Font.BOLD,  26);
    private static final Font F_BODY  = new Font("SansSerif", Font.PLAIN, 17);
    private static final Font F_BTN   = new Font("SansSerif", Font.BOLD,  14);
    private static final Font F_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font F_MONO  = new Font("Monospaced", Font.BOLD, 16);

    // ── Fields ────────────────────────────────────────────
    private final String      nickname;
    private final PrintWriter out;
    private final Gson        gson;

    private JFrame     frame;
    private JPanel     mainPanel;
    private CardLayout cardLayout;

    // Waiting
    private JLabel waitingLabel;

    // Question
    private JLabel         questionLabel;
    private TimerRing      timerRing;
    private AnswerButton[] answerButtons = new AnswerButton[4];
    private int            timerMax = 15;

    // Scoreboard
    private JPanel scoreListPanel;
    private JLabel scoreTitle;

    private static final String S_WAIT  = "WAIT";
    private static final String S_QUEST = "QUEST";
    private static final String S_SCORE = "SCORE";

    // ─────────────────────────────────────────────────────
    public ClientGUI(String nickname, PrintWriter out, Gson gson) {
        this.nickname = nickname;
        this.out      = out;
        this.gson     = gson;
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        buildUI();
    }

    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    // ── Build all screens ─────────────────────────────────
    private void buildUI() {
        frame = new JFrame("KahootJava  ·  " + nickname);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(680, 520);
        frame.setMinimumSize(new Dimension(540, 420));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);

        cardLayout = new CardLayout();
        mainPanel  = new JPanel(cardLayout);
        mainPanel.setBackground(BG);
        mainPanel.add(buildWaitingPanel(),    S_WAIT);
        mainPanel.add(buildQuestionPanel(),   S_QUEST);
        mainPanel.add(buildScoreboardPanel(), S_SCORE);
        frame.add(mainPanel);
        showWaiting("Waiting for other players...");
    }

    // ── 1. Waiting screen ─────────────────────────────────
    private JPanel buildWaitingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);

        JPanel card = new RoundPanel(20, SURFACE, BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(48, 64, 48, 64));

        JLabel dot = new JLabel("◆", SwingConstants.CENTER);
        dot.setFont(new Font("SansSerif", Font.PLAIN, 40));
        dot.setForeground(ACCENT);
        dot.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = label("KahootJava", F_TITLE, TEXT_PRI);
        title.setAlignmentX(CENTER_ALIGNMENT);

        waitingLabel = label("Waiting...", F_BODY, TEXT_SEC);
        waitingLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel you = label("Playing as: " + nickname, F_SMALL, ACCENT);
        you.setAlignmentX(CENTER_ALIGNMENT);

        card.add(dot);
        card.add(vgap(10));
        card.add(title);
        card.add(vgap(18));
        card.add(waitingLabel);
        card.add(vgap(6));
        card.add(you);
        panel.add(card);
        return panel;
    }

    public void showWaiting(String msg) {
        SwingUtilities.invokeLater(() -> {
            waitingLabel.setText(msg);
            cardLayout.show(mainPanel, S_WAIT);
        });
    }

    // ── 2. Question screen ────────────────────────────────
    private JPanel buildQuestionPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 22, 20, 22));

        // Top bar with timer
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG);
        JLabel qlabel = label("Answer the question", F_SMALL, TEXT_SEC);
        top.add(qlabel, BorderLayout.WEST);
        timerRing = new TimerRing();
        top.add(timerRing, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        // Question card
        JPanel qCard = new RoundPanel(16, SURFACE, BORDER);
        qCard.setLayout(new BorderLayout());
        qCard.setBorder(new EmptyBorder(28, 32, 28, 32));
        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setFont(F_BODY);
        questionLabel.setForeground(TEXT_PRI);
        qCard.add(questionLabel, BorderLayout.CENTER);
        panel.add(qCard, BorderLayout.CENTER);

        // 2x2 answer grid
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setBackground(BG);
        String[] letters = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            answerButtons[i] = new AnswerButton(letters[i], BTN_COLORS[i]);
            final String l = letters[i];
            answerButtons[i].addActionListener(e -> sendAnswer(l));
            grid.add(answerButtons[i]);
        }
        panel.add(grid, BorderLayout.SOUTH);
        return panel;
    }

    public void showQuestion(String json) {
        SwingUtilities.invokeLater(() -> {
            Question q = gson.fromJson(json, Question.class);
            questionLabel.setText(
                "<html><div style='text-align:center;line-height:1.5'>" + q.getText() + "</div></html>"
            );
            String[] opts = q.getOptions();
            for (int i = 0; i < 4; i++) {
                answerButtons[i].setOptionText(opts[i]);
                answerButtons[i].setEnabled(true);
                answerButtons[i].setChosen(false);
            }
            timerMax = org.example.shared.GameConfig.QUESTION_TIMER_SECONDS;
            timerRing.reset(timerMax);
            cardLayout.show(mainPanel, S_QUEST);
        });
    }

    public void updateTimer(String seconds) {
        SwingUtilities.invokeLater(() -> {
            int s = Integer.parseInt(seconds);
            timerRing.tick(s, timerMax);
            if (s == 0) disableAll();
        });
    }

    private void sendAnswer(String letter) {
        disableAll();
        String[] ls = {"A","B","C","D"};
        for (int i = 0; i < 4; i++)
            if (ls[i].equals(letter)) answerButtons[i].setChosen(true);
        out.println(new Message(Message.ANSWER, letter).toJson());
    }

    private void disableAll() {
        for (AnswerButton b : answerButtons) b.setEnabled(false);
    }

    // ── 3. Scoreboard screen ──────────────────────────────
    private JPanel buildScoreboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BG);
        scoreTitle = label("Scoreboard", F_TITLE, TEXT_PRI);
        hdr.add(scoreTitle, BorderLayout.WEST);
        panel.add(hdr, BorderLayout.NORTH);

        scoreListPanel = new JPanel();
        scoreListPanel.setLayout(new BoxLayout(scoreListPanel, BoxLayout.Y_AXIS));
        scoreListPanel.setBackground(BG);

        JScrollPane scroll = new JScrollPane(scoreListPanel);
        scroll.setBackground(BG);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public void showScoreboard(String json) {
        SwingUtilities.invokeLater(() -> {
            Type t = new TypeToken<List<PlayerScore>>(){}.getType();
            List<PlayerScore> list = gson.fromJson(json, t);

            scoreTitle.setText("Scoreboard");
            scoreListPanel.removeAll();
            scoreListPanel.add(vgap(4));

            int maxScore = list.stream().mapToInt(PlayerScore::getScore).max().orElse(1);
            String[] medals = {"🥇", "🥈", "🥉"};

            for (int i = 0; i < list.size(); i++) {
                PlayerScore ps  = list.get(i);
                String medal    = i < medals.length ? medals[i] : (i + 1) + ".";
                boolean isMe    = ps.getNickname().equals(nickname);
                scoreListPanel.add(scoreRow(ps, medal, maxScore, isMe));
                scoreListPanel.add(vgap(8));
            }
            scoreListPanel.revalidate();
            scoreListPanel.repaint();
            cardLayout.show(mainPanel, S_SCORE);
        });
    }

    private JPanel scoreRow(PlayerScore ps, String medal, int maxScore, boolean isMe) {
        Color rowBg  = isMe ? new Color(28, 40, 58) : SURFACE;
        Color border = isMe ? new Color(88, 166, 255, 120) : BORDER;

        JPanel row = new RoundPanel(12, rowBg, border);
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(new EmptyBorder(14, 16, 14, 16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        // Left: medal + name + correct count
        JPanel left = new JPanel(new BorderLayout(10, 0));
        left.setOpaque(false);

        JLabel m = label(medal, new Font("SansSerif", Font.PLAIN, 22), TEXT_PRI);
        left.add(m, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel name    = label(ps.getNickname(), F_BTN, isMe ? ACCENT : TEXT_PRI);
        JLabel correct = label(ps.getCorrectAnswers() + " correct answers", F_SMALL, TEXT_SEC);
        info.add(name);
        info.add(vgap(2));
        info.add(correct);
        left.add(info, BorderLayout.CENTER);

        // Right: score + bar
        JPanel right = new JPanel(new BorderLayout(0, 6));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(180, 48));

        JLabel sc = label(ps.getScore() + " pts", F_MONO, TEXT_PRI);
        sc.setHorizontalAlignment(SwingConstants.RIGHT);

        int filled = maxScore > 0 ? (int)(170.0 * ps.getScore() / maxScore) : 0;
        JPanel bar = new ScoreBar(filled, 170, isMe ? ACCENT : new Color(48, 58, 72));

        right.add(sc, BorderLayout.NORTH);
        right.add(bar, BorderLayout.SOUTH);

        row.add(left,  BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    public void showGameOver(String json) {
        SwingUtilities.invokeLater(() -> {
            showScoreboard(json);
            scoreTitle.setText("🏆  Final Results");
            frame.setTitle("KahootJava  ·  Game Over!");
        });
    }

    public void showError(String msg) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    // ── Helpers ───────────────────────────────────────────
    private static JLabel label(String text, Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f); l.setForeground(c); return l;
    }
    private static Component vgap(int h) { return Box.createVerticalStrut(h); }

    // ══════════════════════════════════════════════════════
    // Inner components
    // ══════════════════════════════════════════════════════

    /** Rounded opaque panel */
    static class RoundPanel extends JPanel {
        private final int radius; private final Color bg, border;
        RoundPanel(int r, Color bg, Color border) {
            this.radius=r; this.bg=bg; this.border=border; setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0,0,getWidth(),getHeight(),radius,radius);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);
            g2.dispose(); super.paintComponent(g);
        }
    }

    /** Answer button with accent stripe and hover */
    static class AnswerButton extends JButton {
        private final Color accent;
        private boolean chosen=false, hovered=false;
        private String letter;

        AnswerButton(String letter, Color accent) {
            this.letter=letter; this.accent=accent;
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setForeground(TEXT_PRI);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 72));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e){hovered=true; repaint();}
                public void mouseExited (MouseEvent e){hovered=false;repaint();}
            });
        }

        void setOptionText(String text) { setText(letter + "   " + text); }
        void setChosen(boolean b) { chosen=b; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = chosen ? accent
                : (hovered && isEnabled()) ? brighter(SURFACE2, 15) : SURFACE2;
            g2.setColor(bg);
            g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
            // accent stripe on left
            g2.setColor(chosen ? accent.darker() : accent);
            g2.fillRoundRect(0,0,5,getHeight(),4,4);
            // border
            g2.setColor(chosen ? accent.darker() : BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
            g2.dispose(); super.paintComponent(g);
        }
        private Color brighter(Color c, int amt) {
            return new Color(
                Math.min(c.getRed()+amt,255),
                Math.min(c.getGreen()+amt,255),
                Math.min(c.getBlue()+amt,255));
        }
    }

    /** Circular countdown timer ring */
    static class TimerRing extends JPanel {
        private int cur=15, max=15;
        TimerRing() { setOpaque(false); setPreferredSize(new Dimension(72,72)); }
        void reset(int m) { cur=max=m; repaint(); }
        void tick(int c, int m) { cur=c; max=m; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int pad=8, size=Math.min(getWidth(),getHeight())-pad*2;
            int x=(getWidth()-size)/2, y=(getHeight()-size)/2;
            g2.setStroke(new BasicStroke(5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g2.setColor(BORDER);
            g2.drawOval(x,y,size,size);
            Color arc = cur<=5 ? new Color(255,123,114)
                : cur<=10 ? new Color(210,153,34) : ACCENT;
            g2.setColor(arc);
            int angle = max>0 ? (int)(360.0*cur/max) : 0;
            g2.drawArc(x,y,size,size,90,-angle);
            g2.setFont(new Font("SansSerif",Font.BOLD,size/3));
            g2.setColor(cur<=5 ? new Color(255,123,114) : TEXT_PRI);
            FontMetrics fm=g2.getFontMetrics();
            String txt=String.valueOf(cur);
            g2.drawString(txt,(getWidth()-fm.stringWidth(txt))/2,
                (getHeight()+fm.getAscent()-fm.getDescent())/2);
            g2.dispose();
        }
    }

    /** Thin horizontal score bar */
    static class ScoreBar extends JPanel {
        private final int filled, total; private final Color color;
        ScoreBar(int filled, int total, Color color) {
            this.filled=filled; this.total=total; this.color=color;
            setOpaque(false);
            setPreferredSize(new Dimension(total,6));
            setMaximumSize(new Dimension(total,6));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BORDER); g2.fillRoundRect(0,0,total,6,6,6);
            if(filled>0){g2.setColor(color); g2.fillRoundRect(0,0,filled,6,6,6);}
            g2.dispose();
        }
    }
}
