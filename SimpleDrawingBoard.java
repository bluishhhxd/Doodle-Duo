import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SimpleDrawingBoard { 
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimpleDrawingBoard::createAndShowGUI);
    }

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Doodle Duo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        DrawingBoardPanel drawingBoardPanel = new DrawingBoardPanel();
        ControlPanel controlPanel = new ControlPanel(drawingBoardPanel);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(drawingBoardPanel, BorderLayout.CENTER);
        frame.getContentPane().add(controlPanel, BorderLayout.NORTH);
        frame.pack();
        frame.setVisible(true);
    }
}

enum ShapeType {
    FREEHAND,
    LINE,
    RECTANGLE,
    ELLIPSE,
    TEXT
}

class DrawingBoardPanel extends JPanel {
    public Point prevPoint;
    public Point controlPoint;
    public Point endPoint;
    public boolean isEraserMode = false;
    public int strokeWidth = 4;
    public List<ShapeStroke> shapeStrokes = new ArrayList<>();
    public int currentStrokeIndex = -1;
    public Cursor pencilCursor;
    public double scale = 1.0; // Initial scale
    public int mouseX;
    public int mouseY;
    public String textToAdd;
    public boolean isAddingText = false;
    public ShapeType currentShapeType = ShapeType.FREEHAND;
    public Point startPoint;
    public Color currentColor = new Color(211,211,211); // Default color

    public DrawingBoardPanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(18,18,18));

        // Add MouseWheelListener for smooth zoom in/out
        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                if (notches < 0) {
                    zoomIn(e.getX(), e.getY());
                } else {
                    zoomOut(e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                setCursor(createPencilCursor(strokeWidth));
            }

            public void mouseDragged(MouseEvent e) {
                endPoint = e.getPoint();

                if (prevPoint != null) {
                    if (isAddingText) {
                        repaint();
                        return;
                    }
                    if (currentShapeType == ShapeType.FREEHAND) {
                        drawFreehand();
                    } else {
                        repaint();
                    }
                }

                prevPoint = endPoint;
                controlPoint = new Point((prevPoint.x + endPoint.x) / 2, (prevPoint.y + endPoint.y) / 2);
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                if (currentShapeType == ShapeType.TEXT) {
                    textToAdd = JOptionPane.showInputDialog("Enter text:");
                    if (textToAdd != null) {
                        Graphics2D g = (Graphics2D) getGraphics();
                        g.setFont(new Font("Arial", Font.PLAIN, strokeWidth * 2));
                        FontMetrics fm = g.getFontMetrics();
                        Rectangle2D textBounds = fm.getStringBounds(textToAdd, g);
                        g.setColor(getBackground());
                        g.fillRect((int) startPoint.getX(), (int) startPoint.getY() - fm.getAscent(), (int) textBounds.getWidth(), (int) textBounds.getHeight());
                        g.setColor(isEraserMode ?new Color(18,18,18) : currentColor);
                        g.drawString(textToAdd, (int) startPoint.getX(), (int) startPoint.getY());
                        g.dispose();
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (currentShapeType == ShapeType.FREEHAND) {
                    endPoint = e.getPoint();
                    drawFreehand();
                } else if (currentShapeType == ShapeType.RECTANGLE) {
                    endPoint = e.getPoint();
                    int x = Math.min(startPoint.x, endPoint.x);
                    int y = Math.min(startPoint.y, endPoint.y);
                    int width = Math.abs(startPoint.x - endPoint.x);
                    int height = Math.abs(startPoint.y - endPoint.y);
                    ShapeStroke stroke = new ShapeStroke(new Rectangle2D.Float(x, y, width, height), isEraserMode ? new Color(18,18,18) : currentColor, strokeWidth);
                    addShapeStroke(stroke);
                } else if (currentShapeType == ShapeType.ELLIPSE) {
                    endPoint = e.getPoint();
                    int x = Math.min(startPoint.x, endPoint.x);
                    int y = Math.min(startPoint.y, endPoint.y);
                    int width = Math.abs(startPoint.x - endPoint.x);
                    int height = Math.abs(startPoint.y - endPoint.y);
                    ShapeStroke stroke = new ShapeStroke(new Ellipse2D.Float(x, y, width, height), isEraserMode ? new Color(18,18,18): currentColor, strokeWidth);
                    addShapeStroke(stroke);
                }
                startPoint = null;
                prevPoint = null;
                controlPoint = null;
                endPoint = null;
                repaint();
            }
        });
    }

    public void addShapeStroke(ShapeStroke stroke) {
        // Clear strokes after current index
        if (currentStrokeIndex < shapeStrokes.size() - 1) {
            shapeStrokes.subList(currentStrokeIndex + 1, shapeStrokes.size()).clear();
        }
        shapeStrokes.add(stroke);
        currentStrokeIndex = shapeStrokes.size() - 1;
    }

    public void drawFreehand() {
        QuadCurve2D curve = new QuadCurve2D.Float();
        curve.setCurve(prevPoint.x, prevPoint.y, controlPoint.x, controlPoint.y, endPoint.x, endPoint.y);

        Graphics2D g = (Graphics2D) getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isEraserMode ? new Color(18,18,18) : currentColor);
        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(curve);
        g.dispose();

        // Store the drawn stroke
        ShapeStroke stroke = new ShapeStroke(curve, isEraserMode ? new Color(18,18,18) : currentColor, strokeWidth);
        addShapeStroke(stroke);
    }

    public void setEraserMode(boolean isEraserMode) {
        this.isEraserMode = isEraserMode;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        if (pencilCursor != null) {
            setCursor(null); // Remove previous cursor
        }
        pencilCursor = createPencilCursor(strokeWidth);
        setCursor(pencilCursor); // Set new cursor
    }

    public void setCurrentColor(Color color) {
        this.currentColor = color;
    }

    public Cursor createPencilCursor(int strokeWidth) {
        int size = strokeWidth * 5; // Adjust the size based on the stroke width
        int originalSize = 20; // Original size of the cursor image

        // Scale factor
        double scaleFactor = (double) size / originalSize;

        BufferedImage cursorImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = cursorImage.createGraphics();
        g2d.setColor(currentColor);
        g2d.setStroke(new BasicStroke(2));

        // Draw pencil tip
        g2d.drawLine((int) (2 * scaleFactor), (int) (originalSize / 2 * scaleFactor), (int) (originalSize / 2 * scaleFactor), (int) (2 * scaleFactor));

        // Draw pencil body
        g2d.drawLine((int) (originalSize / 2 * scaleFactor), (int) (2 * scaleFactor), (int) ((originalSize - 5) * scaleFactor), (int) (originalSize / 2 * scaleFactor));
        g2d.drawLine((int) ((originalSize - 5) * scaleFactor), (int) (originalSize / 2 * scaleFactor), (int) (originalSize / 2 * scaleFactor), (int) ((originalSize - 5) * scaleFactor));
        g2d.drawLine((int) (originalSize / 2 * scaleFactor), (int) ((originalSize - 5) * scaleFactor), (int) (2 * scaleFactor), (int) (originalSize / 2 * scaleFactor));

        g2d.dispose();
        return Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point((int) (size / 2), (int) (size / 2)), "Pencil Cursor");
    }

    public void undo() {
        if (currentStrokeIndex >= 0) {
            shapeStrokes.remove(currentStrokeIndex);
            currentStrokeIndex--;
            repaint();
        }
    }

    public void redo() {
        if (currentStrokeIndex < shapeStrokes.size() - 1) {
            currentStrokeIndex++;
            repaint();
        }
    }

    public void clearBoard() {
        shapeStrokes.clear();
        currentStrokeIndex = -1;
        repaint();
    }

    public void zoomIn(int mouseX, int mouseY) {
        scale += 0.1; // Increase scale by 10%
        adjustPosition(mouseX, mouseY);
        revalidate();
        repaint();
    }

    public void zoomOut(int mouseX, int mouseY) {
        scale -= 0.1; // Decrease scale by 10%
        adjustPosition(mouseX, mouseY);
        revalidate();
        repaint();
    }

    public void adjustPosition(int mouseX, int mouseY) {
        int dx = (int) ((mouseX - getWidth() / 2) * 0.1);
        int dy = (int) ((mouseY - getHeight() / 2) * 0.1);
        this.setLocation(this.getX() - dx, this.getY() - dy);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.scale(scale, scale); // Apply scaling transformation
        for (int i = 0; i <= currentStrokeIndex; i++) {
            ShapeStroke stroke = shapeStrokes.get(i);
            g2d.setColor(stroke.getColor());
            g2d.setStroke(new BasicStroke(stroke.getStrokeWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(stroke.getShape());
        }
        if (textToAdd != null && isAddingText) {
            g2d.setColor(getBackground());
            FontMetrics fm = g2d.getFontMetrics();
            Rectangle2D textBounds = fm.getStringBounds(textToAdd, g2d);
            g2d.fillRect((int) prevPoint.getX(), (int) prevPoint.getY() - fm.getAscent(), (int) textBounds.getWidth(), (int) textBounds.getHeight());
            g2d.setColor(isEraserMode ? Color.WHITE : currentColor);
            g2d.drawString(textToAdd, (int) prevPoint.getX(), (int) prevPoint.getY());
        }
    }
}

class ControlPanel extends JPanel {
    public DrawingBoardPanel drawingBoard;
    public JButton eraserButton;
    public JSlider strokeSlider;
    public JComboBox<String> shapeSelector;
    public JButton colorButton;
    public JButton clearButton; // New button for clearing the board

    public ControlPanel(DrawingBoardPanel drawingBoard) {
        this.drawingBoard = drawingBoard;
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        setPreferredSize(new Dimension(400, 50));

        eraserButton = new JButton("Eraser");
        eraserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawingBoard.setEraserMode(!drawingBoard.isEraserMode);
                eraserButton.setText(drawingBoard.isEraserMode ? "Pencil" : "Eraser");
            }
        });
        add(eraserButton);

        strokeSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, 4);
        strokeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                drawingBoard.setStrokeWidth(strokeSlider.getValue());
            }
        });
        strokeSlider.setMajorTickSpacing(1);
        strokeSlider.setPaintTicks(true);
        strokeSlider.setPaintLabels(true);
        add(strokeSlider);

        String[] shapes = {"Freehand", "Line", "Rectangle", "Ellipse", "Text"};
        shapeSelector = new JComboBox<>(shapes);
        shapeSelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedShape = (String) shapeSelector.getSelectedItem();
                switch (selectedShape) {
                    case "Freehand":
                        drawingBoard.currentShapeType = ShapeType.FREEHAND;
                        break;
                    case "Line":
                        drawingBoard.currentShapeType = ShapeType.LINE;
                        break;
                    case "Rectangle":
                        drawingBoard.currentShapeType = ShapeType.RECTANGLE;
                        break;
                    case "Ellipse":
                        drawingBoard.currentShapeType = ShapeType.ELLIPSE;
                        break;
                    case "Text":
                        drawingBoard.currentShapeType = ShapeType.TEXT;
                        break;
                }
            }
        });
        add(shapeSelector);

        colorButton = new JButton("Color");
        colorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color selectedColor = JColorChooser.showDialog(null, "Choose Color", drawingBoard.currentColor);
                if (selectedColor != null) {
                    drawingBoard.setCurrentColor(selectedColor);
                }
            }
        });
        add(colorButton);

        // New button for clearing the board
        clearButton = new JButton("Clear Board");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawingBoard.clearBoard();
            }
        });
        add(clearButton);

        // Add key bindings for undo and redo
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                drawingBoard.undo();
            }
        });
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                drawingBoard.redo();
            }
        });

        // Request focus for the ControlPanel and set focusable
        this.setFocusable(true);
        this.requestFocus();
    }
}

class ShapeStroke {
    public Shape shape;
    public Color color;
    public int strokeWidth;

    public ShapeStroke(Shape shape, Color color, int strokeWidth) {
        this.shape = shape;
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    public Shape getShape() {
        return shape;
    }

    public Color getColor() {
        return color;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }
}
