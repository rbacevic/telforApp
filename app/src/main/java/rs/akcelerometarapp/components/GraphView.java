package rs.akcelerometarapp.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by RADEEE on 26-Nov-15.
 */
public class GraphView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public GraphView(Context context) {
        super(context);

        Log.i(TAG, "GraphView.GraphView()");

        mHolder = getHolder();
        mHolder.addCallback(this);

        setFocusable(true);
        requestFocus();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.i(TAG, "GraphView.surfaceChanged()");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "GraphView.surfaceCreated()");

        mDrawRoop = true;
        mThread = new Thread(this);
        mThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "GraphView.surfaceDestroyed()");

        mDrawRoop = false;
        boolean roop = true;
        while (roop) {
            try {
                mThread.join();
                roop = false;
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        mThread = null;
    }

    @Override
    public void run() {
        Log.i(TAG, "GraphView.run()");

        int width = getWidth();
        int mLineWidth = 2;
        mMaxHistorySize = (width - 20) / mLineWidth;

        Paint textPaint = new Paint();
        textPaint.setColor(mStringColor);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(14);

        Paint zeroLinePaint = new Paint();
        zeroLinePaint.setColor(mZeroLineColor);
        zeroLinePaint.setAntiAlias(true);

        // Iscrtavanje X,Y,Z i R ose i kanvasa za pozadinu
        Paint[] linePaints = new Paint[4];
        for (int i = 0; i < 4; i++) {
            linePaints[i] = new Paint();
            linePaints[i].setColor(mAngleColors[i]);
            linePaints[i].setAntiAlias(true);
            linePaints[i].setStrokeWidth(2);
        }

        while (mDrawRoop) {
            Canvas canvas = mHolder.lockCanvas();

            if (canvas == null) {
                break;
            }

            canvas.drawColor(mBGColor);

            float zeroLineY = mZeroLineY + mZeroLineYOffset;

            synchronized (mHolder) {
                float twoLineY = zeroLineY - (20 * mGraphScale);
                float oneLineY = zeroLineY - (10 * mGraphScale);
                float minasOneLineY = zeroLineY + (10 * mGraphScale);
                float minasTwoLineY = zeroLineY + (20 * mGraphScale);

                canvas.drawText("2", 5, twoLineY + 5, zeroLinePaint);
                canvas.drawLine(20, twoLineY, width, twoLineY,
                        zeroLinePaint);

                canvas.drawText("1", 5, oneLineY + 5, zeroLinePaint);
                canvas.drawLine(20, oneLineY, width, oneLineY,
                        zeroLinePaint);

                canvas.drawText("0", 5, zeroLineY + 5, zeroLinePaint);
                canvas.drawLine(20, zeroLineY, width, zeroLineY,
                        zeroLinePaint);

                canvas.drawText("-1", 5, minasOneLineY + 5, zeroLinePaint);
                canvas.drawLine(20, minasOneLineY, width, minasOneLineY,
                        zeroLinePaint);

                canvas.drawText("-2", 5, minasTwoLineY + 5, zeroLinePaint);
                canvas.drawLine(20, minasTwoLineY, width, minasTwoLineY,
                        zeroLinePaint);

                if (mHistory.size() > 1) {
                    Iterator<float[]> iterator = mHistory.iterator();
                    float[] before = new float[4];
                    int x = width - mHistory.size() * mLineWidth;
                    int beforeX = x;
                    x += mLineWidth;

                    if (iterator.hasNext()) {
                        float[] history = iterator.next();
                        for (int angle = 0; angle < 4; angle++) {
                            before[angle] = zeroLineY
                                    - (history[angle] * mGraphScale);
                        }
                        while (iterator.hasNext()) {
                            history = iterator.next();
                            for (int angle = 0; angle < 4; angle++) {
                                float startY = zeroLineY
                                        - (history[angle] * mGraphScale);
                                float stopY = before[angle];
                                if (mGraphs[angle]) {
                                    canvas.drawLine(x, startY, beforeX,
                                            stopY, linePaints[angle]);
                                }
                                before[angle] = startY;
                            }
                            beforeX = x;
                            x += mLineWidth;
                        }
                    }
                }
            }
            mHolder.unlockCanvasAndPost(canvas);

            try {
                int mDrawDelay = 100;
                Thread.sleep(mDrawDelay);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public int getmZeroLineYOffset() {
        return mZeroLineYOffset;
    }

    public void setmZeroLineYOffset(int mZeroLineYOffset) {
        this.mZeroLineYOffset = mZeroLineYOffset;
    }

    public int getmZeroLineY() {
        return mZeroLineY;
    }

    public void setmZeroLineY(int mZeroLineY) {
        this.mZeroLineY = mZeroLineY;
    }

    public int getmGraphScale() {
        return mGraphScale;
    }

    public void setmGraphScale(int mGraphScale) {
        this.mGraphScale = mGraphScale;
    }

    public boolean ismDrawRoop() {
        return mDrawRoop;
    }

    public void setmDrawRoop(boolean mDrawRoop) {
        this.mDrawRoop = mDrawRoop;
    }

    public int getmMaxHistorySize() {
        return mMaxHistorySize;
    }

    public void setmMaxHistorySize(int mMaxHistorySize) {
        this.mMaxHistorySize = mMaxHistorySize;
    }

    public int getmZeroLineColor() {
        return mZeroLineColor;
    }

    public void setmZeroLineColor(int mZeroLineColor) {
        this.mZeroLineColor = mZeroLineColor;
    }

    public int[] getmAngleColors() {
        return mAngleColors;
    }

    public void setmAngleColors(int[] mAngleColors) {
        this.mAngleColors = mAngleColors;
    }

    public ConcurrentLinkedQueue<float[]> getmHistory() {
        return mHistory;
    }

    public void setmHistory(ConcurrentLinkedQueue<float[]> mHistory) {
        this.mHistory = mHistory;
    }

    public boolean[] getmGraphs() {
        return mGraphs;
    }

    public void setmGraphs(boolean[] mGraphs) {
        this.mGraphs = mGraphs;
    }

    public int getmBGColor() {
        return mBGColor;
    }

    public void setmBGColor(int mBGColor) {
        this.mBGColor = mBGColor;
    }

    public int getmStringColor() {
        return mStringColor;
    }

    public void setmStringColor(int mStringColor) {
        this.mStringColor = mStringColor;
    }

    private Thread mThread;
    private final SurfaceHolder mHolder;
    private ConcurrentLinkedQueue<float[]> mHistory = new ConcurrentLinkedQueue<>();

    private boolean[] mGraphs = {true, true, true, true};
    private int[] mAngleColors = new int[4];

    private int mBGColor;
    private int mZeroLineColor;
    private int mStringColor;

    private int mMaxHistorySize;
    private boolean mDrawRoop = true;
    private int mGraphScale = 6;
    private int mZeroLineY = 230;
    private int mZeroLineYOffset = 0;

    private static final String TAG = "GraphView";
}
