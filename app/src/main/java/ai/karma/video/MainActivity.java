package ai.karma.video;
import android.app.*;import android.os.*;import android.content.*;import android.net.Uri;import android.webkit.*;import android.widget.*;import android.view.*;
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class MainActivity extends Activity {
 WebView home,browser; LinearLayout root; ValueCallback<Uri[]> pending; boolean external=false; boolean exporting=false; Uri[] montage; Uri soundtrack; String engineUrl; int outputWidth=720,outputHeight=1280; androidx.media3.transformer.Transformer audioTransformer;
 @Override public void onCreate(Bundle b){super.onCreate(b);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(0,getResources().getIdentifier("status_bar_height","dimen","android")>0?getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height","dimen","android")):0,0,0);Button back=new Button(this);back.setText("KARMA • Retour au studio");root.addView(back);Button open=new Button(this);open.setText("Ouvrir dans le navigateur système");root.addView(open);Button edit=new Button(this);edit.setText("Assembler des vidéos hors ligne");root.addView(edit);edit.setOnClickListener(v->{if(exporting)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,21);});open.setOnClickListener(v->{String url=browser.getUrl();if(url==null||!url.startsWith("https://"))url=engineUrl;if(external&&url!=null&&url.startsWith("https://")){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){Toast.makeText(this,"Navigateur indisponible",Toast.LENGTH_LONG).show();}}else Toast.makeText(this,"Choisis d’abord un moteur",Toast.LENGTH_SHORT).show();});home=new WebView(this);browser=new WebView(this);setup(home);setup(browser);root.addView(home,new LinearLayout.LayoutParams(-1,0,1));root.addView(browser,new LinearLayout.LayoutParams(-1,0,1));browser.setVisibility(View.GONE);back.setOnClickListener(v->showHome());setContentView(root);home.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){Uri u=r.getUrl();if("https".equals(u.getScheme())&&r.isForMainFrame()){engineUrl=u.toString();external=true;home.setVisibility(View.GONE);browser.setVisibility(View.VISIBLE);browser.loadUrl(u.toString());return true;}return !"file".equals(u.getScheme());}});home.addJavascriptInterface(new LocalClipboard(),"KarmaClipboard"); CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(browser,true);home.loadUrl("file:///android_asset/index.html");}
 private class LocalClipboard { @JavascriptInterface public void copy(String value){runOnUiThread(()->{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("Prompt KARMA",value));});} }
 void showHome(){external=false;browser.setVisibility(View.GONE);home.setVisibility(View.VISIBLE);}
 void setup(WebView w){WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);w.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !"https".equals(r.getUrl().getScheme());}});w.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams p){if(pending!=null)pending.onReceiveValue(null);pending=cb;try{Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT);picker.addCategory(Intent.CATEGORY_OPENABLE);String[] types=p.getAcceptTypes();picker.setType(types.length==1&&types[0].contains("/")?types[0]:"*/*");picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,p.getMode()==WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE);startActivityForResult(picker,12);}catch(Exception e){pending.onReceiveValue(null);pending=null;}return true;}});w.setDownloadListener((url,ua,disposition,mime,len)->{try{if(!"https".equals(Uri.parse(url).getScheme()))throw new IllegalArgumentException();DownloadManager.Request req=new DownloadManager.Request(Uri.parse(url));req.addRequestHeader("User-Agent",ua);String cookie=CookieManager.getInstance().getCookie(url);if(cookie!=null)req.addRequestHeader("Cookie",cookie);req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url,disposition,mime));req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(req);Toast.makeText(this,"Téléchargement lancé",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Téléchargement non pris en charge : utiliser le navigateur",Toast.LENGTH_LONG).show();}});}
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==23){if(c==RESULT_OK&&d!=null&&d.getData()!=null){soundtrack=d.getData();chooseOutput();}return;}if(r==21&&c==RESULT_OK&&d!=null){ClipData cd=d.getClipData();if(cd!=null){montage=new Uri[cd.getItemCount()];for(int j=0;j<montage.length;j++)montage[j]=cd.getItemAt(j).getUri();}else if(d.getData()!=null)montage=new Uri[]{d.getData()};if(montage!=null&&montage.length>=2){new AlertDialog.Builder(this).setTitle("Assemblage MP4").setMessage("Les clips seront assemblés dans l’ordre de sélection. Les formats différents seront convertis si nécessaire. Le son original sera remplacé ou retiré.").setNegativeButton("Annuler",null).setNeutralButton("Sans musique",(dialog,which)->{soundtrack=null;chooseOutput();}).setPositiveButton("Ajouter musique",(dialog,which)->{Intent audio=new Intent(Intent.ACTION_OPEN_DOCUMENT);audio.setType("audio/*");audio.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(audio,23);}).show();}else Toast.makeText(this,"Sélectionne au moins deux clips",Toast.LENGTH_LONG).show();return;}if(r==22&&c==RESULT_OK&&d!=null&&d.getData()!=null&&montage!=null){Uri target=d.getData();Uri[] input=montage.clone();Uri music=soundtrack;exporting=true;Toast.makeText(this,"Assemblage en cours",Toast.LENGTH_SHORT).show();new AlertDialog.Builder(this).setTitle("Format de sortie").setItems(new String[]{"Vertical 720 × 1280","Carré 720 × 720","Horizontal 1280 × 720"},(dialog,which)->{outputWidth=which==2?1280:720;outputHeight=which==0?1280:720;prepareMusic(input,target,music);}).setOnCancelListener(dialog->finishExport("Export annulé")).show();return;}if(r==12&&pending!=null){Uri[] result=null;if(c==RESULT_OK&&d!=null){ClipData clips=d.getClipData();if(clips!=null){result=new Uri[clips.getItemCount()];for(int i=0;i<result.length;i++)result[i]=clips.getItemAt(i).getUri();}else if(d.getData()!=null)result=new Uri[]{d.getData()};}pending.onReceiveValue(result);pending=null;}}


 private void finishExport(String message){exporting=false;if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK",null).show();}
 
 private void runMontage(Uri[] input,Uri target,Uri music,java.io.File temporary){
  java.io.File output=null;
  try{
   output=java.io.File.createTempFile("karma-normalized-",".mp4",getCacheDir());if(!output.delete())throw new java.io.IOException("Cache indisponible");
   final java.io.File normalized=output;
   java.util.ArrayList<androidx.media3.transformer.EditedMediaItem> clips=new java.util.ArrayList<>();
   for(Uri uri:input){
    androidx.media3.transformer.Effects effects=new androidx.media3.transformer.Effects(java.util.Collections.emptyList(),java.util.Collections.singletonList(androidx.media3.effect.Presentation.createForWidthAndHeight(outputWidth,outputHeight,androidx.media3.effect.Presentation.LAYOUT_SCALE_TO_FIT)));
    clips.add(new androidx.media3.transformer.EditedMediaItem.Builder(androidx.media3.common.MediaItem.fromUri(uri)).setRemoveAudio(true).setEffects(effects).build());
   }
   androidx.media3.transformer.EditedMediaItemSequence sequence=new androidx.media3.transformer.EditedMediaItemSequence(clips);
   androidx.media3.transformer.Composition composition=new androidx.media3.transformer.Composition.Builder(sequence).build();
   Toast.makeText(this,"Normalisation et assemblage vidéo en cours",Toast.LENGTH_LONG).show();
   audioTransformer=new androidx.media3.transformer.Transformer.Builder(this).setVideoMimeType(androidx.media3.common.MimeTypes.VIDEO_H264).addListener(new androidx.media3.transformer.Transformer.Listener(){
    @Override public void onCompleted(androidx.media3.transformer.Composition composition,androidx.media3.transformer.ExportResult result){
     audioTransformer=null;new Thread(()->{String message;try{
      if(music==null){try(java.io.InputStream in=new java.io.FileInputStream(normalized);java.io.OutputStream out=getContentResolver().openOutputStream(target,"wt")){if(out==null)throw new java.io.IOException("Destination indisponible");byte[] bytes=new byte[65536];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);}}
      else joinVideos(new Uri[]{Uri.fromFile(normalized)},target,music);
      message="MP4 enregistré";
     }catch(Exception e){message="Échec : "+e.getMessage();}finally{normalized.delete();if(temporary!=null)temporary.delete();}final String msg=message;runOnUiThread(()->finishExport(msg));}).start();
    }
    @Override public void onError(androidx.media3.transformer.Composition composition,androidx.media3.transformer.ExportResult result,androidx.media3.transformer.ExportException error){audioTransformer=null;normalized.delete();if(temporary!=null)temporary.delete();finishExport("Conversion vidéo impossible : "+error.getMessage());}
   }).build();
   audioTransformer.start(composition,normalized.getAbsolutePath());
  }catch(Exception e){audioTransformer=null;if(output!=null)output.delete();if(temporary!=null)temporary.delete();finishExport("Montage impossible : "+e.getMessage());}
 }

 private void prepareMusic(Uri[] input,Uri target,Uri music){
  if(music==null){runMontage(input,target,null,null);return;}
  boolean aac=false;android.media.MediaExtractor probe=new android.media.MediaExtractor();
  try{probe.setDataSource(this,music,null);for(int i=0;i<probe.getTrackCount();i++){if("audio/mp4a-latm".equals(probe.getTrackFormat(i).getString(android.media.MediaFormat.KEY_MIME))){aac=true;break;}}}catch(Exception e){finishExport("Musique illisible : "+e.getMessage());return;}finally{probe.release();}
  if(aac){runMontage(input,target,music,null);return;}
  try{
   final java.io.File temp=java.io.File.createTempFile("karma-audio-",".mp4",getCacheDir());if(!temp.delete())throw new java.io.IOException("Cache indisponible");
   Toast.makeText(this,"Conversion locale de la musique en AAC",Toast.LENGTH_SHORT).show();
   androidx.media3.transformer.EditedMediaItem item=new androidx.media3.transformer.EditedMediaItem.Builder(androidx.media3.common.MediaItem.fromUri(music)).setRemoveVideo(true).build();
   audioTransformer=new androidx.media3.transformer.Transformer.Builder(this).setAudioMimeType(androidx.media3.common.MimeTypes.AUDIO_AAC).addListener(new androidx.media3.transformer.Transformer.Listener(){
    @Override public void onCompleted(androidx.media3.transformer.Composition composition,androidx.media3.transformer.ExportResult result){audioTransformer=null;runMontage(input,target,Uri.fromFile(temp),temp);}
    @Override public void onError(androidx.media3.transformer.Composition composition,androidx.media3.transformer.ExportResult result,androidx.media3.transformer.ExportException error){audioTransformer=null;temp.delete();finishExport("Conversion audio impossible : "+error.getMessage());}
   }).build();
   audioTransformer.start(item,temp.getAbsolutePath());
  }catch(Exception e){audioTransformer=null;finishExport("Conversion audio impossible : "+e.getMessage());}
 }
 private void chooseOutput(){Intent out=new Intent(Intent.ACTION_CREATE_DOCUMENT);out.setType("video/mp4");out.addCategory(Intent.CATEGORY_OPENABLE);out.putExtra(Intent.EXTRA_TITLE,"KARMA-montage.mp4");startActivityForResult(out,22);}
 private void joinVideos(Uri[] sources,Uri target,Uri music) throws Exception {
  java.io.File temp=java.io.File.createTempFile("karma-",".mp4",getCacheDir());
  android.media.MediaMuxer mux=null;boolean started=false; android.media.MediaExtractor audio=null;
  try {
   mux=new android.media.MediaMuxer(temp.getAbsolutePath(),android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
   android.media.MediaFormat first=null; int track=-1; int audioTrack=-1; long offset=0;
   if(music!=null){audio=new android.media.MediaExtractor();audio.setDataSource(this,music,null);for(int j=0;j<audio.getTrackCount();j++){android.media.MediaFormat af=audio.getTrackFormat(j);if("audio/mp4a-latm".equals(af.getString(android.media.MediaFormat.KEY_MIME))){audioTrack=mux.addTrack(af);audio.selectTrack(j);break;}}if(audioTrack<0)throw new java.io.IOException("Musique AAC/M4A requise; MP3 non pris en charge dans cette version");}
   java.nio.ByteBuffer buffer=java.nio.ByteBuffer.allocateDirect(16*1024*1024);
   for(Uri uri:sources){
    android.media.MediaExtractor ex=new android.media.MediaExtractor();
    try{
     ex.setDataSource(this,uri,null);int idx=-1;android.media.MediaFormat fmt=null;
     for(int n=0;n<ex.getTrackCount();n++){android.media.MediaFormat candidate=ex.getTrackFormat(n);String mime=candidate.getString(android.media.MediaFormat.KEY_MIME);if(mime!=null&&mime.startsWith("video/")){idx=n;fmt=candidate;break;}}
     if(idx<0||fmt==null)throw new java.io.IOException("Aucune piste vidéo");
     if(!"video/avc".equals(fmt.getString(android.media.MediaFormat.KEY_MIME)))throw new java.io.IOException("Seuls les clips H.264 sont acceptés");
     if(first==null){first=fmt;track=mux.addTrack(fmt);if(fmt.containsKey("rotation-degrees"))mux.setOrientationHint(fmt.getInteger("rotation-degrees"));mux.start();started=true;}
     else {for(String key:new String[]{"width","height","rotation-degrees"}){int x=first.containsKey(key)?first.getInteger(key):0;int y=fmt.containsKey(key)?fmt.getInteger(key):0;if(x!=y)throw new java.io.IOException("Dimensions ou orientation différentes");}for(String key:new String[]{"csd-0","csd-1"}){if(!java.util.Objects.equals(first.getByteBuffer(key),fmt.getByteBuffer(key)))throw new java.io.IOException("Encodages H.264 différents");}}
     ex.selectTrack(idx);long base=ex.getSampleTime(),last=-1,previous=-1;if(base<0||(ex.getSampleFlags()&android.media.MediaExtractor.SAMPLE_FLAG_SYNC)==0)throw new java.io.IOException("Clip sans image clé initiale");
     while(true){long pts=ex.getSampleTime();if(pts<0)break;if(ex.getSampleSize()>buffer.capacity())throw new java.io.IOException("Image compressée trop volumineuse");buffer.clear();int size=ex.readSampleData(buffer,0);if(size<0)break;long rel=pts-base;if(rel<0)throw new java.io.IOException("Horodatage non compatible");android.media.MediaCodec.BufferInfo info=new android.media.MediaCodec.BufferInfo();info.set(0,size,offset+rel,(ex.getSampleFlags()&android.media.MediaExtractor.SAMPLE_FLAG_SYNC)!=0?android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME:0);mux.writeSampleData(track,buffer,info);if(rel>last){previous=last;last=rel;}ex.advance();}
     if(last<0)throw new java.io.IOException("Clip vide");long frame=previous>=0?last-previous:33333;offset+=last+Math.max(1,frame);
    }finally{ex.release();}
   }
   if(audio!=null){long base=audio.getSampleTime();if(base<0)throw new java.io.IOException("Musique vide");while(audio.getSampleTime()>=0){long pts=audio.getSampleTime()-base;if(pts>=offset)break;if(audio.getSampleSize()>buffer.capacity())throw new java.io.IOException("Échantillon audio trop volumineux");buffer.clear();int size=audio.readSampleData(buffer,0);if(size<0)break;android.media.MediaCodec.BufferInfo info=new android.media.MediaCodec.BufferInfo();info.set(0,size,pts,0);mux.writeSampleData(audioTrack,buffer,info);audio.advance();}}
   mux.stop();started=false;mux.release();mux=null;
   try(java.io.InputStream in=new java.io.FileInputStream(temp);java.io.OutputStream out=getContentResolver().openOutputStream(target,"wt")){if(out==null)throw new java.io.IOException("Destination indisponible");byte[] bytes=new byte[65536];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);}
  }finally{if(audio!=null)audio.release();if(mux!=null){if(started)try{mux.stop();}catch(Exception ignored){}mux.release();}temp.delete();}
 }
 @Override public void onBackPressed(){if(external){if(browser.canGoBack())browser.goBack();else showHome();}else super.onBackPressed();}
}
