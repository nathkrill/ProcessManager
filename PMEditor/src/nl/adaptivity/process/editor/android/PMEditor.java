package nl.adaptivity.process.editor.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static nl.adaptivity.diagram.Drawable.*;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.android.AndroidPen;
import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.diagram.android.DiagramView.DiagramDrawable;
import nl.adaptivity.diagram.android.DiagramView.OnNodeClickListener;
import nl.adaptivity.process.diagram.DiagramNode;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.diagram.LayoutStepper;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

public class PMEditor extends Activity implements OnNodeClickListener {

  private static final int STATE_ACTIVE=STATE_CUSTOM1;
  private static final int STATE_GROUP=STATE_CUSTOM2;
  private static final int STATE_XMOST=STATE_CUSTOM3;
  private static final int STATE_MOVED=STATE_CUSTOM4;



  private final class LineDrawable extends DiagramDrawable {

    private double aX1;
    private double aY1;
    private double aX2;
    private double aY2;
    private final Paint aPaint;

    public LineDrawable(double pX1, double pY1, double pX2, double pY2, Paint pPaint) {
      aX1 = pX1;
      aX2 = pX2;
      aY1 = pY1;
      aY2 = pY2;
      aPaint = pPaint;
    }

    @Override
    public void draw(Canvas pCanvas, double pScale) {
      int height = (int) Math.round((pCanvas.getHeight()/pScale));
      int width = (int) Math.round(pCanvas.getWidth()/pScale);
      if (!Double.isNaN(aX1)) {
        float scaledX = (float) (aX1-diagramView1.getOffsetX());
        pCanvas.drawLine(scaledX, 0, scaledX, height, aPaint);
      }
      if (!Double.isNaN(aY1)) {
        float scaledY = (float) (aY1-diagramView1.getOffsetY());
        pCanvas.drawLine(0, scaledY, width, scaledY, aPaint);
      }
      if (!Double.isNaN(aX2)) {
        float scaledX = (float) (aX2-diagramView1.getOffsetX());
        pCanvas.drawLine(scaledX, 0, scaledX, height, aPaint);
      }
      if (!Double.isNaN(aY2)) {
        float scaledY = (float) (aY2-diagramView1.getOffsetY());
        pCanvas.drawLine(0, scaledY, width, scaledY, aPaint);
      }
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSPARENT;
    }

    @Override
    public void setAlpha(int pAlpha) {
      //
    }

    @Override
    public void setColorFilter(ColorFilter pCf) {
      //
    }

  }

  private final static LayoutAlgorithm<DrawableProcessNode> NULL_LAYOUT_ALGORITHM = new LayoutAlgorithm<DrawableProcessNode>(){

    @Override
    public boolean layout(List<? extends DiagramNode<DrawableProcessNode>> pNodes) {
      return false; // don't do any layout
    }

  };

  private static class MoveDrawable extends DiagramDrawable{

    private int aAlpha = 255;
    private List<float[]> arrows;
    private Paint aPaint;
    private Drawable aMinMaxOverlay;

    public MoveDrawable(Drawable pMinMaxOverlay, List<float[]> pArrows) {
      aMinMaxOverlay = pMinMaxOverlay;
      arrows = pArrows;
    }

    @Override
    public void draw(Canvas pCanvas, double pScale) {
      if (aMinMaxOverlay!=null) {
        if (aMinMaxOverlay instanceof DiagramDrawable) {
          ((DiagramDrawable)aMinMaxOverlay).draw(pCanvas, pScale);
        } else {
          aMinMaxOverlay.draw(pCanvas);
        }
      }
      if (aPaint ==null) {
        aPaint = new Paint();
        aPaint.setAntiAlias(true);
        aPaint.setAlpha(aAlpha);
        aPaint.setStyle(Style.STROKE);
        aPaint.setStrokeWidth(3);
        aPaint.setARGB(255, 0, 255, 0);
      }
      for(float[] arrow:arrows) {
        pCanvas.drawLine(arrow[0], arrow[1], arrow[2], arrow[3], aPaint);
      }
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSPARENT;
    }

    @Override
    public void setAlpha(int pAlpha) {
      aAlpha = pAlpha;
      if (aPaint!=null) {
        aPaint.setAlpha(pAlpha);
      }
    }

    @Override
    public void setColorFilter(ColorFilter pCf) {
      // ignore
    }

  }

  private static class WaitTask extends FutureTask<Object> {

    private static final Callable<Object> NULLCALLABLE = new Callable<Object>() {

      @Override
      public Object call() throws Exception {
        throw new UnsupportedOperationException("Should not be set");
      }

    };

    private final boolean aImmediate;

    private Drawable aOverlay;

    public WaitTask(boolean pImmediate, Drawable pOverlay) {
      super(NULLCALLABLE);
      aImmediate = pImmediate;
      aOverlay = pOverlay;
    }

    public void trigger() {
      set(Boolean.TRUE);
    }

    public boolean isImmediate() {
      return aImmediate;
    }
  }

  private class MyStepper extends LayoutStepper<DrawableProcessNode> {

    private boolean aImmediate = false;
    private AndroidPen aXMostPen;
    private DiagramNode<DrawableProcessNode> aLayoutNode;
    private double aMinX = Double.NaN;
    private double aMinY = Double.NaN;
    private double aMaxX = Double.NaN;
    private double aMaxY = Double.NaN;
    private LineDrawable aMinMaxOverlay = null;

    @Override
    public void reportLayoutNode(DiagramNode<DrawableProcessNode> pNode) {
      aMinX = Double.NaN;
      aMinY = Double.NaN;
      aMaxX = Double.NaN;
      aMaxY = Double.NaN;
      aMinMaxOverlay = null;
      setLabel("node under consideration");
      if (! (Double.isNaN(pNode.getX()) || Double.isNaN(pNode.getY()))) {
        if (aLayoutNode!=null) {
          final DrawableProcessNode target = aLayoutNode.getTarget();
          target.setState(target.getState()& ~STATE_ACTIVE);
        }
        aLayoutNode = pNode;
        {
          final DrawableProcessNode target = aLayoutNode.getTarget();
          target.setState(target.getState()|STATE_ACTIVE);
        }
        waitForNextClicked(null);
      }
    }

    @Override
    public void reportLowest(List<? extends DiagramNode<DrawableProcessNode>> pNodes, DiagramNode<DrawableProcessNode> pNode) {
      setLabel("lowest");
      reportXMost(pNodes, pNode);
    }

    @Override
    public void reportHighest(List<? extends DiagramNode<DrawableProcessNode>> pNodes, DiagramNode<DrawableProcessNode> pNode) {
      setLabel("highest");
      reportXMost(pNodes, pNode);
    }

    @Override
    public void reportRightmost(List<? extends DiagramNode<DrawableProcessNode>> pNodes, DiagramNode<DrawableProcessNode> pNode) {
      setLabel("rightmost");
      reportXMost(pNodes, pNode);
    }

    @Override
    public void reportLeftmost(List<? extends DiagramNode<DrawableProcessNode>> pNodes, DiagramNode<DrawableProcessNode> pNode) {
      setLabel("leftmost");
      reportXMost(pNodes, pNode);
    }

    @Override
    public void reportMinX(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pX) {
      setLabel("minX");
      aMinX = pX;
      reportMinMax(pNodes);
    }

    @Override
    public void reportMaxX(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pX) {
      setLabel("maxX");
      aMaxX=pX;
      reportMinMax(pNodes);
    }

    @Override
    public void reportMinY(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pY) {
      setLabel("minY");
      aMinY = pY;
      reportMinMax(pNodes);
    }

    @Override
    public void reportMaxY(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pY) {
      setLabel("maxY");
      aMaxY = pY;
      reportMinMax(pNodes);
    }

    private void reportMinMax(List<? extends DiagramNode<DrawableProcessNode>> pNodes) {
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()|STATE_GROUP);
      }
      if (aMinMaxOverlay==null) {
        aMinMaxOverlay = new LineDrawable(aMinX, aMinY, aMaxX, aMaxY, getLinePen().getPaint());
      } else {
        aMinMaxOverlay.aX1 = aMinX;
        aMinMaxOverlay.aY1 = aMinY;
        aMinMaxOverlay.aX2 = aMaxX;
        aMinMaxOverlay.aY2 = aMaxY;
      }
      waitForNextClicked(aMinMaxOverlay);
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~STATE_GROUP);
      }
    }

    private void reportXMost(List<? extends DiagramNode<DrawableProcessNode>> pNodes, DiagramNode<DrawableProcessNode> pNode) {
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX());
        target.setY(node.getY());

        if (node!=pNode) {
          target.setState(target.getState()|STATE_GROUP);
        }
      }
      if (pNode!=null) {
        final DrawableProcessNode target = pNode.getTarget();
        target.setState(target.getState()|STATE_XMOST);
      }
      updateDiagramBounds();
      waitForNextClicked(aMinMaxOverlay);
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~(STATE_XMOST|STATE_GROUP));
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void reportMove(DiagramNode<DrawableProcessNode> pNode, double pNewX, double pNewY) {
      setLabel("move");
      final DrawableProcessNode target = pNode.getTarget();
      target.setState(target.getState()|STATE_MOVED);
      target.setX(pNewX);
      target.setY(pNewY);
      updateDiagramBounds();

      waitForNextClicked(moveDrawable(aMinMaxOverlay, Arrays.asList(pNode)));

      target.setState(target.getState()& ~STATE_MOVED);
      aMinX = Double.NaN;
      aMinY = Double.NaN;
      aMaxX = Double.NaN;
      aMaxY = Double.NaN;
      aMinMaxOverlay = null;
    }

    private MoveDrawable moveDrawable(Drawable pMinMaxOverlay, List<? extends DiagramNode<?>> pNodes) {
      List<float[]> arrows = new ArrayList<float[]>(pNodes.size());
      for(DiagramNode<?> pNode: pNodes) {
        if (! (Double.isNaN(pNode.getX())|| Double.isNaN(pNode.getY()))) {
          final double scale = diagramView1.getScale();
          arrows.add(new float[]{(float) ((pNode.getX()+(diagramView1.getOffsetX()*scale))*scale),
                                 (float) ((pNode.getY()+(diagramView1.getOffsetY()*scale))*scale),
                                 (float) ((pNode.getTarget().getX()+(diagramView1.getOffsetX()*scale))*scale),
                                 (float) ((pNode.getTarget().getY()+(diagramView1.getOffsetY()*scale))*scale)});
        }
      }
      return new MoveDrawable(pMinMaxOverlay, arrows);
    }



    @Override
    public void reportMoveX(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pOffset) {
      setLabel("moveX");
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX()+pOffset);
        target.setY(node.getY());
        target.setState(target.getState()|STATE_MOVED);
      }
      updateDiagramBounds();
      waitForNextClicked(moveDrawable(aMinMaxOverlay, pNodes));
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~STATE_MOVED);
      }
    }

    @Override
    public void reportMoveY(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pOffset) {
      setLabel("moveY");
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX());
        target.setY(node.getY()+pOffset);
        target.setState(target.getState()|STATE_MOVED);
      }
      updateDiagramBounds();
      waitForNextClicked(moveDrawable(aMinMaxOverlay, pNodes));
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~STATE_MOVED);
      }
    }

    private void waitForNextClicked(Drawable pOverlay) {
      if (aLayoutTask!=null) {
        if (Thread.currentThread()==Looper.getMainLooper().getThread()) {
          if (BuildConfig.DEBUG) {
            throw new IllegalStateException("Performing layout on UI thread");
          }
        } else {
          final WaitTask task = new WaitTask(aImmediate, pOverlay);
          aLayoutTask.postProgress(task);
          try {
            task.get();
          } catch (ExecutionException e) { // ignore
          } catch (InterruptedException e) { // ignore
          }
        }
      }
    }

    private AndroidPen getLinePen() {
      if (aXMostPen ==null) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setARGB(255, 255, 0, 0);
        aXMostPen = new AndroidPen(paint);
      }
      return aXMostPen;
    }

  }

  private class LayoutTask extends AsyncTask<Object, WaitTask, Object> {

    private final MyStepper aStepper = new MyStepper();
    private WaitTask aTask;

    @Override
    protected Object doInBackground(Object... pParams) {
      // Start with null layout algorithm, to prevent dual layout.
      aPm = getProcessModel(NULL_LAYOUT_ALGORITHM);
      if (aPm!=null) {
        LayoutAlgorithm<DrawableProcessNode> alg = new LayoutAlgorithm<DrawableProcessNode>();
        alg.setLayoutStepper(aStepper);
        aPm.setLayoutAlgorithm(alg);
        aAdapter = new MyDiagramAdapter(aPm);
        diagramView1.setAdapter(aAdapter);
        aPm.layout();
      }
      return null;
    }

    public void postProgress(WaitTask pWaitTask) {
      this.publishProgress(pWaitTask);
    }

    @Override
    protected void onProgressUpdate(WaitTask... pValues) {
      final WaitTask task = pValues[0];
      if (! isCancelled()) {
  //      findViewById(R.id.ac_next).setEnabled(true);
        diagramView1.setOverlay(task.aOverlay);
        diagramView1.invalidate();
      }
      if (task.isImmediate()) {
        task.trigger();
      }
      aTask = task;
    }

    @Override
    protected void onPostExecute(Object pResult) {
      aLayoutTask=null;
      updateDiagramBounds();
      if (aStepper.aLayoutNode!=null) {
        final DrawableProcessNode target = aStepper.aLayoutNode.getTarget();
        target.setState(target.getState()& (~(STATE_CUSTOM1|STATE_CUSTOM2|STATE_CUSTOM3|STATE_CUSTOM4)));
        aStepper.aLayoutNode = null;
      }
      diagramView1.setOverlay(null);
      diagramView1.invalidate();
    }

    public void playAll() {
      aStepper.aImmediate= true;
      if (aTask!=null) {
        aTask.trigger();
        aTask = null;
//        findViewById(R.id.ac_next).setEnabled(false);
      }
    }

    public void next() {
      if (aTask!=null) {
        aTask.trigger();
        aTask = null;
//        findViewById(R.id.ac_next).setEnabled(false);
      }
    }

  }

  boolean aStep = true;


  private DiagramView diagramView1;


  private DrawableProcessModel aPm;
  
  private MyDiagramAdapter aAdapter;

  private LayoutTask aLayoutTask;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    diagramView1 = (DiagramView) findViewById(R.id.diagramView1);
    diagramView1.setScale(2d);
    diagramView1.setOffsetX(0d);
    diagramView1.setOffsetY(0d);
    diagramView1.setOnNodeClickListener(this);
  }

  public void setLabel(final String pString) {
    runOnUiThread(new Runnable(){

      @Override
      public void run() {
        getActionBar().setTitle("PMEditor - " +pString);
      }

    });

  }

  public void updateDiagramBounds() {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for(DrawableProcessNode node: aPm.getModelNodes()) {
      if (!(Double.isNaN(node.getX()) || Double.isNaN(node.getY()))) {
        final Rectangle bounds = node.getBounds();
        if (bounds.left<minX) { minX = bounds.left; }
        if (bounds.top<minY) { minY = bounds.top; }
        if (bounds.bottom()>maxY) { maxY = bounds.bottom(); }
      }
    }
    double offsetX= Double.isInfinite(minX)? 0 : minX - aPm.getLeftPadding();
    double offsetY= Double.isInfinite(minY)? 0 : minY - aPm.getTopPadding();
    diagramView1.setOffsetX(offsetX/*/diagramView1.getScale()*/);
    diagramView1.setOffsetY(offsetY-(((diagramView1.getHeight()/diagramView1.getScale())-(maxY-minY))/2));
  }

  private final DrawableProcessModel getProcessModel() {
    return getProcessModel(new LayoutAlgorithm<DrawableProcessNode>());
  }

  private DrawableProcessModel getProcessModel(LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm) {
    final InputStream file = getResources().openRawResource(R.raw.processmodel);
    try {
      // Start with null layout algorithm, to prevent dual layout.
      return PMParser.parseProcessModel(file, layoutAlgorithm);
    } finally {
      try {
        file.close();
      } catch (IOException e) {
        Log.e(PMEditor.class.getName(), e.getMessage(), e);
        return null;
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    aPm = getProcessModel();
    aAdapter = new MyDiagramAdapter(aPm);
    diagramView1.setAdapter(aAdapter);
  }



  @Override
  protected void onDestroy() {
    if (aLayoutTask!=null) {
      aLayoutTask.playAll();
      aLayoutTask.cancel(false);
    }
    super.onDestroy();
  }

  @Override
  public boolean onMenuItemSelected(int pFeatureId, MenuItem pItem) {
    switch (pItem.getItemId()) {
      case R.id.ac_next:
        if (aLayoutTask!=null) {
          aLayoutTask.next();
        } else {
          aLayoutTask = new LayoutTask();
          aLayoutTask.execute();
        }
        break;
      case R.id.ac_play:
        if (aLayoutTask!=null) {
          aLayoutTask.playAll();
        }
        break;
      default:
        return false;
    }
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu pMenu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.diagram_menu, pMenu);
    return true;
  }

  @Override
  public boolean onNodeClicked(DiagramView pView, int position, MotionEvent pEvent) {
    pView.setSelection(position);
    return true;
  }


}
