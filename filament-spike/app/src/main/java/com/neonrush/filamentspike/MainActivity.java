package com.neonrush.filamentspike;
import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.filament.Engine;
import com.google.android.filament.gltfio.Gltfio;
import com.google.android.filament.Skybox;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.utils.ModelViewer;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
public final class MainActivity extends Activity implements Choreographer.FrameCallback {
 private ModelViewer viewer; private TextView status; private boolean running; private long windowStart; private int frames;
 @Override public void onCreate(Bundle b){super.onCreate(b);
  FrameLayout root=new FrameLayout(this); SurfaceView surface=new SurfaceView(this); root.addView(surface,new FrameLayout.LayoutParams(-1,-1));
  status=new TextView(this);status.setTextColor(-1);status.setTextSize(12);status.setPadding(18,14,18,14);status.setBackgroundColor(0xDD050A12);status.setText("NEON RUSH • FILAMENT DIAGNOSTIC\nLoading corridor…");
  ScrollView panel=new ScrollView(this);panel.setFillViewport(false);panel.addView(status);
  FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);lp.topMargin=12;lp.leftMargin=12;lp.rightMargin=12;root.addView(panel,lp);setContentView(root);
  try{Gltfio.init();viewer=new ModelViewer(surface, Engine.create(), new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK), null);
   viewer.getCamera().lookAt(0.0, 2.0, 8.0, 0.0, 2.0, -3.5, 0.0, 1.0, 0.0);
   viewer.getScene().setSkybox(new Skybox.Builder().build(viewer.getEngine()));viewer.getScene().getSkybox().setColor(.008f,.015f,.028f,1f);
   byte[] encoded=readAsset("corridor.glb.b64");byte[] glb=Base64.decode(new String(encoded,java.nio.charset.StandardCharsets.US_ASCII),Base64.DEFAULT);
   viewer.loadModelGlb(ByteBuffer.wrap(glb));status.setText("NEON RUSH • FILAMENT TEST\nFixed camera • FPS: waiting");
  }catch(Throwable e){showFailure("INITIALIZATION",e);}
 }
 private void showFailure(String stage,Throwable e){running=false;String detail=Log.getStackTraceString(e);status.setText("FILAMENT DIAGNOSTIC — "+stage+" FAILED\n\n"+e.getClass().getName()+": "+String.valueOf(e.getMessage())+"\n\n"+detail);Log.e("NeonRushFilament",stage+" failed",e);}
 private byte[] readAsset(String n)throws Exception{try(InputStream in=getAssets().open(n);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int k;while((k=in.read(b))>0)out.write(b,0,k);return out.toByteArray();}}
 @Override protected void onResume(){super.onResume();if(viewer!=null&&!running){running=true;windowStart=System.nanoTime();Choreographer.getInstance().postFrameCallback(this);}}
 @Override public void doFrame(long t){if(!running||viewer==null)return;try{boolean rendered=viewer.render(t);if(rendered)frames++;long elapsed=t-windowStart;if(elapsed>=1000000000L){int fps=Math.round(frames*1000000000f/elapsed);status.setText("NEON RUSH • FILAMENT TEST\nFixed camera • FPS: "+fps);frames=0;windowStart=t;}Choreographer.getInstance().postFrameCallback(this);}catch(Throwable e){showFailure("RENDER LOOP",e);}}
 @Override protected void onPause(){running=false;Choreographer.getInstance().removeFrameCallback(this);super.onPause();}
 @Override protected void onDestroy(){running=false;if(viewer!=null){try{viewer.destroy();}catch(Throwable e){Log.e("NeonRushFilament","Destroy failed",e);}viewer=null;}super.onDestroy();}
}