package com.kaidongyuan.app.kdydriver.ui.activity;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.kaidongyuan.app.basemodule.interfaces.AsyncHttpCallback;
import com.kaidongyuan.app.basemodule.utils.nomalutils.NetworkUtils;
import com.kaidongyuan.app.basemodule.widget.MLog;
import com.kaidongyuan.app.basemodule.widget.loadingDialog.MyLoadingDialog;
import com.kaidongyuan.app.kdydriver.R;
import com.kaidongyuan.app.kdydriver.bean.Tools;
import com.kaidongyuan.app.kdydriver.constants.Constants;
import com.kaidongyuan.app.kdydriver.httpclient.OrderAsyncHttpClient;
import com.kaidongyuan.app.kdydriver.ui.base.BaseActivity;
import com.kaidongyuan.app.kdydriver.ui.base.BaseFragmentActivity;
import com.kaidongyuan.app.kdydriver.utils.AES256Utils;
import com.ta.utdid2.android.utils.AESUtils;

//打印
import zpSDK.zpSDK.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.view.Menu;

import android.graphics.Canvas;

import zpSDK.zpSDK.zpBluetoothPrinter;

public class AirLabelPrinting extends BaseActivity implements AsyncHttpCallback {

    protected MyLoadingDialog myLoadingDialog;
    public static BluetoothAdapter myBluetoothAdapter;
    public String SelectedBDAddress;
    public static Context mContext;
    public OrderAsyncHttpClient mClient;
    private final String Tag_Get_Locations = "Update_Print_Count";

    StatusBox statusBox;
    /**
     * 返回上一界面按钮
     */

    View view;
    View viewTwo;
    View viewTwoother;
    String json_print;

    // 打印的json_array
    com.alibaba.fastjson.JSONArray json_array_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_first_air);

        AlertDialog alertDialog2 = new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("请先在手机设置的蓝牙中连接打印机设备")
//                    .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        finish();
                    }
                })
                .create();

        if(!ListBluetoothDevice()){
            alertDialog2.show();
        };

        mContext = this;
        mClient = new OrderAsyncHttpClient(AirLabelPrinting.this, this);
        Intent intent1 = getIntent();
        json_print = intent1.getStringExtra("omsNo");

        json_array_data = JSONArray.parseArray(json_print);

        Button Button1 = (Button) findViewById(R.id.air_btPrintCustom);
        statusBox = new StatusBox(this,Button1);
        Button1.setOnClickListener(new Button.OnClickListener()
        {
            public void onClick(View arg0) {

                new Thread() {
                    public void run() {

                        Print1(SelectedBDAddress);
                    }
                }.start();
            }
        });

        Button Button2 = (Button) findViewById(R.id.air_returnjsbtn);

        statusBox = new StatusBox(this,Button2);
        Button2.setOnClickListener(new Button.OnClickListener()
        {
            public void onClick(View arg0) {
                finish();
            }
        });

    }
    @Override
    public void postSuccessMsg(String msg, String request_tag) {

        Log.d("LM", "标签" + request_tag + "请求成功：" + msg);

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    //    打印
    public void Print1(String BDAddress)
    {
//        Looper.prepare();//增加部分
        if(BDAddress == null){

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Toast.makeText(mContext,"请选择要连接的设备", Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showLoadingDialog();
            }
        });

        zpBluetoothPrinter zpSDK=new zpBluetoothPrinter(this);
        if(!zpSDK.connect(BDAddress))
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    cancelLoadingDialog();
                    Toast.makeText(mContext,"连接失败------", Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        try{

            json_array_data = JSONArray.parseArray(json_print);

            JSONObject orderInfo = (JSONObject) json_array_data.get(0);

            view =  getLayoutInflater().inflate(R.layout.airlabel_print_layout, null);

            // 记录是否打印和打印次数
//            Map<String, String> params = new HashMap<String, String>();
//            params.put("omsNo", orderInfo.getString("omsNo"));

//            Map<String, String> param = new HashMap<String, String>();

//            param.put("params", AES256Utils.encrypt(Constants.SecretKey,JSONObject.toJSONString(params)));
//            mClient.setShowToast(false);
//            mClient.sendRequest(Constants.URL.SAAS_API_BASE + "updatePrintCount.do", param, Tag_Get_Locations,false);

//            Log.d("LM", "记录打印次数2"+param );

            //  生成二维码
            ImageView imageviewqrcode45 = (ImageView) view.findViewById(R.id.air_imageviewbarcode);

            Bitmap imagebitmap = createQRCode(orderInfo.getString("newQRCode"), 400);
            imageviewqrcode45.setImageBitmap(imagebitmap);

            //   始发机场三字码
            TextView flightDepcode = (TextView) view.findViewById(R.id.air_departure);
            flightDepcode.setText(orderInfo.getString("flightDepcode"));

            //   始发机场城市
            TextView startCity = (TextView) view.findViewById(R.id.air_startCity);
            startCity.setText("/" + orderInfo.getString("startCity"));

            //   到达机场三字码
            TextView flightArrcode = (TextView) view.findViewById(R.id.air_arrival);
            flightArrcode.setText(orderInfo.getString("flightArrcode"));

            //   到达机场城市
            TextView endCity = (TextView) view.findViewById(R.id.air_endCity);
            endCity.setText("/" + orderInfo.getString("endCity"));

            //   航班号
            TextView actualFlightNo = (TextView) view.findViewById(R.id.air_flightNo);
            actualFlightNo.setText(orderInfo.getString("actualFlightNo"));

            //   航班日期
            TextView flightDate = (TextView) view.findViewById(R.id.air_flightDate);
            flightDate.setText(orderInfo.getString("airlineDate"));

        }catch (Exception e){
            Log.d("LM", "onCreate: ");
        }

        JSONArray scanLabelInfo = (JSONArray) json_array_data.get(1);

        JSONObject scanLabelItem;
        for (int i = 0; i < scanLabelInfo.size(); i++) {

            scanLabelItem = (JSONObject) scanLabelInfo.get(i);

            //  生成条形码
            ImageView imageviewbarcode = (ImageView) view.findViewById(R.id.air_imageView);

            Bitmap barcodebitmap = createBarcode(scanLabelItem.getString("productNo"),400);
            imageviewbarcode.setImageBitmap(barcodebitmap);

            //   标签号
            TextView productno = (TextView) view.findViewById(R.id.air_tagNum);
            productno.setText(scanLabelItem.getString("productNo"));

            int me = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);

            view.measure(me,me);

            Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            if (Build.VERSION.SDK_INT >= 11) {
                view.measure(View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(view.getHeight(), View.MeasureSpec.EXACTLY));
                view.layout((int) view.getX(), (int) view.getY(), (int) view.getX() + view.getMeasuredWidth(), (int) view.getY() + view.getMeasuredHeight());
            } else {
                view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            }
            view.draw(canvas);

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            //设置想要的大小
            int newWidth=770;//96.25   800
            int newHeight=1144;//1155  144.375
//            int newHeight=1200;//1155  144.375

            //计算压缩的比率
            float scaleWidth=((float)newWidth)/width;
            float scaleHeight=((float)newHeight)/height;

            //获取想要缩放的matrix
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth,scaleHeight);
            matrix.postRotate(180);

            //获取新的bitmap
            bitmap=Bitmap.createBitmap(bitmap,0,0,width,height,matrix,true);
            bitmap.getWidth();
            bitmap.getHeight();

            zpSDK.Draw_Page_Bitmap_(bitmap,1);//打印标签，打印大数据图片，宽度不要超过576

            zpSDK.printerStatus();
            int a=zpSDK.GetStatus();

//            if(a==-1)
//            { //"获取状态异常------";
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(mContext,"获取状态异常------", Toast.LENGTH_LONG).show();
//                    }
//                });
//
//                zpSDK.disconnect();
//                break;
//            }
            if(a==1)
            {//"缺纸----------";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"缺纸----------", Toast.LENGTH_LONG).show();
                    }
                });

                zpSDK.disconnect();
                break;
            }
            if(a==2)
            {
                //"开盖----------";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"开盖----------", Toast.LENGTH_LONG).show();
                    }
                });
                zpSDK.disconnect();
                break;
            }
            if(a==0 && i==scanLabelInfo.size()-1)
            {
                //"打印机正常-------";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"已成功打印", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cancelLoadingDialog();
            }
        });
        zpSDK.disconnect();
    }

    public boolean ListBluetoothDevice()
    {

        final List<Map<String,String>> list=new ArrayList<Map<String, String>>();

        ListView listView = (ListView) findViewById(R.id.air_listView1);

        SimpleAdapter m_adapter = new SimpleAdapter( this,list,
                android.R.layout.simple_list_item_2,
                new String[]{"DeviceName","BDAddress"},
                new int[]{android.R.id.text1,android.R.id.text2}
        );

        listView.setAdapter(m_adapter);

        if((myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter())==null)
        {
            Toast.makeText(this,"没有找到蓝牙适配器", Toast.LENGTH_LONG).show();
            return false;
        }

        if(!myBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 2);
        }

        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() <= 0)return false;
        for (BluetoothDevice device : pairedDevices)
        {
            Map<String,String> map=new HashMap<String, String>();
            map.put("DeviceName", device.getName());
            map.put("BDAddress", device.getAddress());
            list.add(map);
        }
        listView.setOnItemClickListener(new ListView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
            {
                SelectedBDAddress = list.get(arg2).get("BDAddress");
                if (((ListView)arg0).getTag() != null){
                    ((View)((ListView)arg0).getTag()).setBackgroundDrawable(null);
                }
                ((ListView)arg0).setTag(arg1);
                arg1.setBackgroundColor(getResources().getColor(R.color.mineblue));

            }
        });
        return true;
    }

    //    @Override
    public void showLoadingDialog() {
        if (myLoadingDialog == null) {
            myLoadingDialog = new MyLoadingDialog(this);
        }
//        cancelLoadingDialog();
        myLoadingDialog.showDialog();
    }

    //    @Override
    public void cancelLoadingDialog() {
        if (myLoadingDialog != null && myLoadingDialog.isShowing()) {
            myLoadingDialog.dismiss();
        }
    }

    //    生成条形码
    public static Bitmap createBarcode(String content,int cheight) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, 2000, cheight);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? 0xff000000 : 0xFFFFFFFF;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }
    //    生成二维码
    public static Bitmap createQRCode(String text, int size) {
        try {
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix bitMatrix = new QRCodeWriter().encode(text,BarcodeFormat.QR_CODE, size, size, hints);
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * size + x] = 0xff000000;
                    } else {
                        pixels[y * size + x] = 0xffffffff;
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

}
