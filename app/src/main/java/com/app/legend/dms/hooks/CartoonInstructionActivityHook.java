package com.app.legend.dms.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.legend.dms.model.Char;
import com.app.legend.dms.model.CharInfo;
import com.app.legend.dms.utils.Conf;
import com.app.legend.dms.utils.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * hook漫画详情页面，在此处可以看漫画
 */
public class CartoonInstructionActivityHook extends BaseHook implements IXposedHookLoadPackage {

    private static final String CLASS="com.dmzj.manhua.ui.CartoonInstructionActivity";

    private static final String METHOD="g";

    private static final String HOST="http://dms.legic.xyz:9006";
    private static final String DEBUG="http://192.168.0.5:9006";

    private String author="";
    private String name;
    private String id;
    private String cover;
    private List<String> stringList;
    private Activity activity;
    private Map<String,JSONObject> objectMap;

    private final static int CONNECT_TIMEOUT =60;
    private final static int READ_TIMEOUT=100;
    private final static int WRITE_TIMEOUT=60;
    String description="";

    String status="";
    String letter="";

    private SQLiteDatabase sqLiteDatabase;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals(Conf.PACKAGE)){
            return;
        }

        XposedHelpers.findAndHookMethod(CLASS, lpparam.classLoader, METHOD, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Activity activity= (Activity) param.thisObject;
                Intent intent=activity.getIntent();

                if (intent!=null){

                    id=intent.getStringExtra("intent_extra_cid");
                    name=intent.getStringExtra("intent_extra_cname");

                }

                ImageView book= (ImageView) XposedHelpers.getObjectField(param.thisObject,"u");//获取封面imageview

                book.setOnClickListener(v -> {



                    if (activity==null){
                        return;
                    }

//                    JSONObject jsonObject=new JSONObject();
//
//                    try {
//                        jsonObject.put("ac",o.toString());
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }



                    //获取chapters数组


                    //获取数组内的对象






//                    if (queryData(id)){//查询是否已经存在，是则不反应
//
//                        Toast.makeText(activity, "别点了，这本漫画已经被收录了", Toast.LENGTH_SHORT).show();
//
//                        return;
//
//                    }

                    //避免id为0
                    if (id.equals("0")){
                        Toast.makeText(activity, "id不可以为0哦~", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Object o=XposedHelpers.getObjectField(param.thisObject,"ac");//获取ac


                    if (o!=null) {
                        cover = (String) XposedHelpers.getObjectField(o, "cover");
                        name= (String) XposedHelpers.getObjectField(o,"title");

                    }

                    TextView t = (TextView) XposedHelpers.getObjectField(param.thisObject,"v");

                    if (t!=null) {
                        author = t.getText().toString();
                    }

                    TextView z= (TextView) XposedHelpers.getObjectField(param.thisObject,"z");

                    if (z!=null){

                        status=z.getText().toString();

                    }
                    description= (String) XposedHelpers.getObjectField(o,"description");

                    letter= (String) XposedHelpers.getObjectField(o,"first_letter");


                    showDialog(activity);

                });


            }
        });

        XposedHelpers.findAndHookMethod(CLASS, lpparam.classLoader, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                activity= (Activity) param.thisObject;

                initList();//实例化

                getLocalChar();

            }
        });



        XposedHelpers.findAndHookMethod(CLASS, lpparam.classLoader, "c", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Activity activity= (Activity) param.thisObject;
                Intent intent=activity.getIntent();

                if (intent!=null){

                    id=intent.getStringExtra("intent_extra_cid");
                    name=intent.getStringExtra("intent_extra_cname");

                }

                String info=getLocalInfos();

                XposedBridge.log("info--->>>"+info);

                if (info!=null&&!TextUtils.isEmpty(info)&&!info.equals("null")){

                    new Thread(){
                        @Override
                        public void run() {
                            super.run();

                            JSONObject object= null;
                            try {
                                object = new JSONObject(info);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                            JSONObject finalObject = object;
                            Runnable runnable= () -> {
                                if (finalObject !=null){

                                    XposedHelpers.callMethod(param.thisObject, "a", finalObject, false);

                                }
                            };

                            activity.runOnUiThread(runnable);
                        }
                    }.start();

                }else {


                    getLocalChar();

//                XposedBridge.log("map--->>>"+objectMap.size());

                    JSONObject object = getObject(id);


//                XposedBridge.log("map--->>>"+objectMap);

                    if (object != null) {//表示已收藏该下架漫画，显示出来


//                    XposedBridge.log("show---->>>"+object.toString());
                        XposedHelpers.callMethod(param.thisObject, "a", object, false);


                        addLocalInfo(id,object.toString());


                    }
                }

            }
        });


        /**
         * 获取sqLiteDatabase实例，操作数据库
         */
        XposedHelpers.findAndHookConstructor("com.dmzj.manhua.e.a.g", lpparam.classLoader, "com.dmzj.manhua.e.a",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);

                        Object object = param.args[0];

                        sqLiteDatabase = (SQLiteDatabase) XposedHelpers.callMethod(object, "getWritableDatabase");


                    }
                });


    }


    /**
     * 展示窗口，表明是否上传漫画信息
     * @param activity
     */
    private void showDialog(Activity activity){

        AlertDialog.Builder builder=new AlertDialog.Builder(activity);

        builder.setTitle("上传说明").setMessage("嘿，兄dei，恭喜你发现了新天地。这个封面在点击过后，可以上传这本漫画信息到共享库里，" +
                "共享库里存储着大量被大妈封印的漫画，如果你发现了一本被封印的漫画，欢迎你将这本漫画的信息上传到共享库内，让更多的人能够阅读到这本漫画。" +
                "当然，我并不会获取你的个人信息，上传的信息仅仅是这本漫画的id，名字以及作者，共享库也在GitHub上随时可以查阅。你可以选择上传普通信息或是上传章节信息，上传章节信息会将普通信息也" +
                "一并上传。但是这种上传只限已下架的漫画，如果你发现某一本漫画被下架了，就可以通过这种方式共享已有的章节信息，让被大妈下架的漫画重新被人看到！")
                .setPositiveButton("上传普通信息",(dialog, which) -> {

                    JSONObject jsonObject=new JSONObject();

                    try {
                        jsonObject.put(Conf.COMIC_NAME,name);
                        jsonObject.put(Conf.COMIC_ID,id);
                        jsonObject.put(Conf.AUTHOR,author);
                        jsonObject.put(Conf.COMIC_BOOK,cover);
                        jsonObject.put("first_letter",letter);
                        jsonObject.put("status",status);
                        jsonObject.put("description",description);

                        uploadAndSave(activity,jsonObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


        }).setNeutralButton("上传章节信息",(dialog, which) -> {


            getCharInfo();



        }).setNegativeButton("取消",(dialog, which) -> {

            Toast.makeText(activity, "打扰了，如果以后发现被隐藏的漫画，还可以继续点击封面进行上传哦~", Toast.LENGTH_LONG).show();

        }).show();
    }


    private void uploadAndSave(Activity activity, JSONObject jsonObject){

        OkHttpClient client= new OkHttpClient.Builder()
                .readTimeout(READ_TIMEOUT,TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)//设置写的超时时间
                .connectTimeout(CONNECT_TIMEOUT,TimeUnit.SECONDS)//设置连接超时时间
                .build();

        String url=HOST+"/v1/save-comic-info";


        RequestBody body=RequestBody.create(MediaType.parse("application/json;charset=utf-8"),jsonObject.toString());

        XposedBridge.log(url);

        Request request=new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

//                Toast.makeText(activity, "发生了点意外，可能是后端服务器崩了~", Toast.LENGTH_SHORT).show();

                e.printStackTrace();

                Runnable runnable= () -> Toast.makeText(activity, "发生了点意外，可能是后端服务器崩了~", Toast.LENGTH_SHORT).show();

                activity.runOnUiThread(runnable);

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {


                if (response.body()!=null) {
//                    String msg = response.body().string();

                    Runnable runnable = () -> Toast.makeText(activity, "上传成功，感谢你的分享~", Toast.LENGTH_SHORT).show();

                    activity.runOnUiThread(runnable);

//                Toast.makeText(activity, ""+msg, Toast.LENGTH_SHORT).show();
                }

            }
        });


    }





    /**
     * 查询该漫画是否已经存在
     */
    private boolean queryData(String id){

        boolean result=false;

        if (this.stringList!=null){

            for (String s:stringList){

                if (s.equals(id)){
                    result=true;
                    break;
                }

            }


        }

        return result;

    }

    /**
     *
     */
    private void initList() {
        if (stringList == null) {
            stringList = new ArrayList<>();

            try {
                File file = new File(activity.getFilesDir(), "comic");

                StringBuilder builder = new StringBuilder();


                if (file.exists()) {//文件存在，解析当前文件

                    FileInputStream inputStream = activity.openFileInput("comic");

                    InputStreamReader reader = new InputStreamReader(inputStream);
                    BufferedReader buffReader = new BufferedReader(reader);
                    String strTmp = "";
                    while ((strTmp = buffReader.readLine()) != null) {
//                    System.out.println(strTmp);
                        builder.append(strTmp);
                    }

                    String json = builder.toString();//获取json信息

                    stringList=JsonUtil.getIdList(json);//获取id_list

                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }

    }

    /**
     * 获取章节信息
     *
     */
    private void getCharInfo(){

//        uploadAndSave(activity,jsonObject);//上传

        Toast.makeText(activity, "正在上传，请稍后", Toast.LENGTH_SHORT).show();

        new Thread(){

            @Override
            public void run() {
                super.run();


                if (sqLiteDatabase==null){
                    return;
                }

                String sql="select * from commic_cache where commic_id = '"+id+"' limit 1";//查询数据库里的对应id的值

                Cursor cursor=sqLiteDatabase.rawQuery(sql,null);

                String info="";

                if (cursor!=null){

                    if (cursor.moveToFirst()){

                        do {

                            info=cursor.getString(cursor.getColumnIndex("commic_info"));


                        }while (cursor.moveToNext());


                    }


                    cursor.close();

                }



                uploadJsonChapterFile(info);

            }
        }.start();



//        XposedBridge.log("json--->>>"+jsonObject.toString());


    }

    /**
     * 获取本地的章节信息
     *
     */
    private void getLocalChar(){

        if (this.objectMap==null) {


            try {
                File file = new File(AndroidAppHelper.currentApplication().getFilesDir(), "chapter");

                StringBuilder builder = new StringBuilder();


                if (file.exists()) {//文件存在，解析当前文件

                    FileInputStream inputStream = AndroidAppHelper.currentApplication().openFileInput("chapter");

                    InputStreamReader reader = new InputStreamReader(inputStream);
                    BufferedReader buffReader = new BufferedReader(reader);
                    String strTmp = "";
                    while ((strTmp = buffReader.readLine()) != null) {
//                    System.out.println(strTmp);
                        builder.append(strTmp);
                    }

                    String json = builder.toString();//获取json信息
                    this.objectMap = JsonUtil.getJsonObjectList(json);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * 获取本地章节信息
     * @param id 传入id
     * @return 返回json对象
     */
    private JSONObject getObject(String id){

        if (this.objectMap==null||id.equals("0")){

            return null;

        }

        return objectMap.get(id);
    }


    private void uploadJsonChapterFile(String json){

        File file=new File(AndroidAppHelper.currentApplication().getFilesDir(),"ccc");//生成文件

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(json); // \r\n即为换行
            out.flush(); // 把缓存区内容压入文件
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        MediaType mediaType = MediaType.parse("text/x-markdown; charset=utf-8");
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(READ_TIMEOUT,TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)//设置写的超时时间
                .connectTimeout(CONNECT_TIMEOUT,TimeUnit.SECONDS)//设置连接超时时间
                .build();

        String url=HOST+"/v1/upload-chapter";

//        XposedBridge.log("url--->>>"+url);

        RequestBody requestBody = RequestBody.create(MediaType.parse("text/x-markdown"), file);


        MultipartBody multipartBody = new MultipartBody.Builder()
                // 设置type为"multipart/form-data"，不然无法上传参数
                .setType(MultipartBody.FORM)
                .addFormDataPart("json", "cc", requestBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(multipartBody)
                .build();


        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                e.printStackTrace();

                XposedBridge.log("e---->>>>"+e.toString());

                Runnable runnable= () -> Toast.makeText(activity, "发生了点意外，可能是后端服务器崩了~", Toast.LENGTH_SHORT).show();

                activity.runOnUiThread(runnable);

                file.delete();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {


                Runnable runnable= () -> Toast.makeText(activity, "上传成功", Toast.LENGTH_SHORT).show();

                activity.runOnUiThread(runnable);

                file.delete();

            }
        });
    }


    /**
     * 优先
     * 从本地获取信息并展示
     * @return
     */
    private String getLocalInfos(){

        if (sqLiteDatabase==null){
            return null;
        }

        String sql="select * from commic_cache where commic_id = '"+id+"' limit 1";

        Cursor cursor=sqLiteDatabase.rawQuery(sql,null);

        String info="";

        if (cursor!=null){

            if (cursor.moveToFirst()){

                do {

                    info=cursor.getString(cursor.getColumnIndex("commic_info"));

                }while (cursor.moveToNext());

            }


            cursor.close();
        }

        return info;

    }


    /**
     * 将记录添加到本地，适用于章节显示
     */
    private void addLocalInfo(String id,String data){

        if (sqLiteDatabase==null){
            return;
        }

        String sql="select commic_id from commic_cache where commic_id = '"+id+"' limit 1";

        Cursor cursor=sqLiteDatabase.rawQuery(sql,null);

        if (cursor!=null){

            if (!cursor.moveToFirst()){//表示为空

                String s1="insert into commic_cache (commic_id,commic_info,version) values ('"+id+"','"+data+"',2)";

                sqLiteDatabase.execSQL(s1);//插入数据
            }

            cursor.close();
        }


    }


}
