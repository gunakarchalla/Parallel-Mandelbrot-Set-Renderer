// The base code has been adapted from https://web.archive.org/web/20221218100830/http://java.rubikscube.info/s0urce/Mandel3.java
// Author: Gunakar Challa
import java.util.Scanner;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public final class Main extends JFrame
        implements MouseListener, MouseMotionListener, KeyListener, WindowListener {
    private int maxCount = 192;
    private boolean smooth = false;
    private boolean antialias = false;
    private boolean drag = false;
    private boolean toDrag = false;
    private boolean rect = true, oldRect = true;
    private Color[][] colors;
    private int pal = 0;
    private double viewX = 0.0;
    private double viewY = 0.0;
    private double zoom = 1.0;

    private int mouseX, mouseY;
    private int dragX, dragY, oldX, oldY;

    volatile AtomicInteger newBuffer=new AtomicInteger();
    volatile BufferedImage image;
    long startTime;
    long endTime;
    static ExecutorService threadPool;
    static int nThreads;
    static int imagePartitions;
    boolean showImage;

    private static final int[][][] colpal = {
            { {0, 10, 20}, {50, 100, 240}, {20, 3, 26}, {230, 60, 20},
                    {25, 10, 9}, {230, 170, 0}, {20, 40, 10}, {0, 100, 0},
                    {5, 10, 10}, {210, 70, 30}, {90, 0, 50}, {180, 90, 120},
                    {0, 20, 40}, {30, 70, 200} },
            { {70, 0, 20}, {100, 0, 100}, {255, 0, 0}, {255, 200, 0} },
            { {40, 70, 10}, {40, 170, 10}, {100, 255, 70}, {255, 255, 255} },
            { {0, 0, 0}, {0, 0, 255}, {0, 255, 255}, {255, 255, 255}, {0, 128, 255} },
            { {0, 0, 0}, {255, 255, 255}, {128, 128, 128} },
    };

    public Main() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        showImage=false;
        setVisible(true);
        setLayout(new FlowLayout());
        addWindowListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        // initialize color palettes
        colors = new Color[colpal.length][];
        for (int p = 0; p < colpal.length; p++) {
            colors[p] = new Color[colpal[p].length * 12];
            for (int i = 0; i < colpal[p].length; i++) {
                int[] c1 = colpal[p][i];
                int[] c2 = colpal[p][(i + 1) % colpal[p].length];
                for (int j = 0; j < 12; j++)
                    colors[p][i * 12 + j] = new Color(
                            (c1[0] * (11 - j) + c2[0] * j) / 11,
                            (c1[1] * (11 - j) + c2[1] * j) / 11,
                            (c1[2] * (11 - j) + c2[2] * j) / 11);
            }
        }
    }

    public static void main(String[] args)
    {
        System.out.println("Enter the number of threads to be created:");
        nThreads=new Scanner(System.in).nextInt();
        threadPool =Executors.newFixedThreadPool(nThreads); // creating thread pool with fixed number of threads
        imagePartitions=nThreads*4; // setting image partitions
        System.out.println("Starting with "+nThreads+" threads and "+imagePartitions+" partitions of the image...");
        SwingUtilities.invokeLater(
                () -> new Main() // starting the Swing JFrame
        );
    }

    public void windowClosing(WindowEvent e)
    {
        threadPool.shutdown(); // closing the threads
        dispose();
        System.exit(0);
    }
    public void windowOpened(WindowEvent e)
    { }
    public void windowIconified(WindowEvent e)
    { }
    public void windowClosed(WindowEvent e)
    { }
    public void windowDeiconified(WindowEvent e)
    { }
    public void windowActivated(WindowEvent e)
    { }
    public void windowDeactivated(WindowEvent e)
    { }

    public void paint(Graphics g) {
        if(showImage){
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(image, 0, 0, null);
            showImage=false;
            endTime=System.currentTimeMillis(); // noting down end time stamp after rendering
            System.out.println("Time taken using "+nThreads+" threads is "+ (endTime-startTime)/1000.0+"s");
            return;
        }
        Dimension size = getSize();
        // select-rectangle or offset-line drawing
        if (drag) {
            g.setColor(Color.black);
            g.setXORMode(Color.white);
            if (oldRect) {
                int x = Math.min(mouseX, oldX);
                int y = Math.min(mouseY, oldY);
                int w = Math.max(mouseX, oldX) - x;
                int h = Math.max(mouseY, oldY) - y;
                double r = Math.max((double)w / size.width, (double)h / size.height);
                g.drawRect(x, y, (int)(size.width * r), (int)(size.height * r));
            }
            else
                g.drawLine(mouseX, mouseY, oldX, oldY);
            if (rect) {
                int x = Math.min(mouseX, dragX);
                int y = Math.min(mouseY, dragY);
                int w = Math.max(mouseX, dragX) - x;
                int h = Math.max(mouseY, dragY) - y;
                double r = Math.max((double)w / size.width, (double)h / size.height);
                g.drawRect(x, y, (int)(size.width * r), (int)(size.height * r));
            }
            else
                g.drawLine(mouseX, mouseY, dragX, dragY);
            oldX = dragX;
            oldY = dragY;
            oldRect = rect;
            drag = false;
            return;
        }

        image=new BufferedImage(size.width,size.height,BufferedImage.TYPE_INT_RGB); //buffered image for parallel calculation
        newBuffer.set(imagePartitions);// for keeping count of completion of threads
        showImage=false; // for showing image after completion of all threads
        SwingWorker<Void,Void>[] workers=new SubPaint[imagePartitions]; // defining workers

        startTime=System.currentTimeMillis(); // noting down start time stamp before partitioning
        for (int i = 0; i < imagePartitions; i++) {
            workers[i]=new SubPaint(size,i*size.height/imagePartitions,(i+1)*size.height/imagePartitions); // dividing image into partitions and assigning to thread pool
            threadPool.submit(workers[i]); // assigning tasks/partitions to thread pool and invoking thread pool
        }
    }

    // fractal image drawing thorugh threads
    public class SubPaint extends SwingWorker{
        static Dimension size;
        int start;
        int end;

        SubPaint(Dimension size, int start, int end){
            this.size=size; // size of the frame
            this.start=start; // start of each partition
            this.end=end; // end of each partition
        }

        @Override
        protected Object doInBackground() throws Exception { // parallel calculation of image
            for (int y = start; y < end; y++) {
                for (int x = 0; x < size.width; x++) {
                    double r = zoom / Math.min(size.width, size.height);
                    double dx = 2.5 * (x * r + viewX) - 2;
                    double dy = 1.25 - 2.5 * (y * r + viewY);
                    Color color = color(dx, dy);
                    // computation of average color for antialiasing
                    if (antialias) {
                        Color c1 = color(dx, dy + 0.5 * r);
                        Color c2 = color(dx + 0.5 * r, dy);
                        Color c3 = color(dx + 0.5 * r, dy + 0.5 * r);
                        int red = (color.getRed() + c1.getRed() + c2.getRed() + c3.getRed()) / 4;
                        int green = (color.getGreen() + c1.getGreen() + c2.getGreen() + c3.getGreen()) / 4;
                        int blue = (color.getBlue() + c1.getBlue() + c2.getBlue() + c3.getBlue()) / 4;
                        color = new Color(red, green, blue);
                    }
                    image.setRGB(x,y,color.getRGB()); // no synchronization is required as parallel setting of different independent pixels of the image, we can verify the same in the rendered image
                }
            }
            return null;
        }

        @Override
        protected void done() { // invoked after every task/partition completion
            if(newBuffer.decrementAndGet()==0){
                showImage=true; // after completion of all partitions the image is shown
                repaint();
            }
        }
    }


    // Computes a color for a given point
    private Color color(double x, double y) {
        int count = mandel(0.0, 0.0, x, y);
        int palSize = colors[pal].length;
        Color color = colors[pal][count / 256 % palSize];
        if (smooth) {
            Color color2 = colors[pal][(count / 256 + palSize - 1) % palSize];
            int k1 = count % 256;
            int k2 = 255 - k1;
            int red = (k1 * color.getRed() + k2 * color2.getRed()) / 255;
            int green = (k1 * color.getGreen() + k2 * color2.getGreen()) / 255;
            int blue = (k1 * color.getBlue() + k2 * color2.getBlue()) / 255;
            color = new Color(red, green, blue);
        }
        return color;
    }

    // Computes a value for a given complex number
    private int mandel(double zRe, double zIm, double pRe, double pIm) {
        double zRe2 = zRe * zRe;
        double zIm2 = zIm * zIm;
        double zM2 = 0.0;
        int count = 0;
        while (zRe2 + zIm2 < 4.0 && count < maxCount) {
            zM2 = zRe2 + zIm2;
            zIm = 2.0 * zRe * zIm + pIm;
            zRe = zRe2 - zIm2 + pRe;
            zRe2 = zRe * zRe;
            zIm2 = zIm * zIm;
            count++;
        }
        if (count == 0 || count == maxCount)
            return 0;
        // transition smoothing
        zM2 += 0.000000001;
        return 256 * count + (int)(255.0 * Math.log(4 / zM2) / Math.log((zRe2 + zIm2) / zM2));
    }

    // methods from MouseListener interface

    public void mousePressed(MouseEvent e) {
        mouseX = dragX = oldX = e.getX();
        mouseY = dragY = oldY = e.getY();
        toDrag = true;
    }

    public void mouseReleased(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
            if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) { // moved
                int width = getSize().width;
                int height = getSize().height;
                viewX += zoom * (mouseX - x) / Math.min(width, height);
                viewY += zoom * (mouseY - y) / Math.min(width, height);
                repaint();
            }
            else if (x != mouseX && y != mouseY) { // zoomed
                int width = getSize().width;
                int height = getSize().height;
                int mx = Math.min(x, mouseX);
                int my = Math.min(y, mouseY);
                viewX += zoom * mx / Math.min(width, height);
                viewY += zoom * my / Math.min(width, height);
                int w = Math.max(x, mouseX) - mx;
                int h = Math.max(y, mouseY) - my;
                double r = Math.max((double)w / width, (double)h / height);
                zoom *= r;
                drag = false;
                repaint();
            }
        }
        else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
            maxCount += maxCount / 4;
            repaint();
        }
        toDrag = false;
    }

    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    // methods from MouseMotionListener interface

    public void mouseDragged(MouseEvent e) {
        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
            dragX = e.getX();
            dragY = e.getY();
            drag = true;
            repaint();
        }
    }
    public void mouseMoved(MouseEvent e) {}

    // methods from KeyListener interface

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { // init
            maxCount = 192;
            viewX = viewY = 0.0;
            zoom = 1.0;
            repaint();
        }
        else if (e.getKeyCode() == KeyEvent.VK_O) { // zoom out
            viewX -= 0.5 * zoom;
            viewY -= 0.5 * zoom;
            zoom *= 2.0;
            repaint();
        }
        else if (e.getKeyCode() == KeyEvent.VK_P) { // next palette
            pal = (pal + 1) % colors.length;
            repaint();
        }
        else if (e.getKeyCode() == KeyEvent.VK_S) { // smoothing
            smooth = !smooth;
            repaint();
        }
        else if (e.getKeyCode() == KeyEvent.VK_A) { // antialiasing
            antialias = !antialias;
            repaint();
        }
        else if (e.getKeyCode() == KeyEvent.VK_SHIFT) { // move/zoom mode
            if (rect == true) {
                oldRect = true;
                rect = false;
                if (toDrag) {
                    drag = true;
                    repaint();
                }
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) { // move/zoom mode
            if (rect == false) {
                oldRect = false;
                rect = true;
                if (toDrag) {
                    drag = true;
                    repaint();
                }
            }
        }
    }

    public void keyTyped(KeyEvent e) {}
}