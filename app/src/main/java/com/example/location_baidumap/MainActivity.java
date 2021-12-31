package com.example.location_baidumap;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.RequiresApi;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.io.File;

/**
 * @author Abby
 * @version V1.0
 **/
public class MainActivity extends Activity {
    private MapView mMapView = null;
    private BaiduMap mBaiduMap = null;
    private RadioGroup mapTypeGroup = null;
    private String mapType = null;
    private String locationMode = null;
    private LocationClient mLocationClient = null;
    private boolean isFirstLoc = true;
    private Button settingsButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMap(); //初始化地图
        initSettings(); //初始化上次的设置
        settings(); //和设置页面交互
        changeMapType(); //功能一：切换地图类型
        showLocation(); //功能二：显示定位+用户设置定位模式
    }

    /**
     * 地图初始化
     */
    protected void initMap(){
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
        //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
        SDKInitializer.setCoordType(CoordType.BD09LL);

        //法一：通过layout文件中添加MapView控件来展示地图
        setContentView(R.layout.activity_main);
        //获取地图控件引用
        mMapView = findViewById(R.id.bMapView);
        //获取地图控制对象
        mBaiduMap = mMapView.getMap();

        /* 法二：直接在Java代码中添加MapView的方式来展示地图
        mMapView = new MapView(this);
        setContentView(mMapView);
        */

        //开启地图的定位图层
        mBaiduMap.setMyLocationEnabled(true);
    }

    /**
     * 初始化上次的设置
     */
    protected void initSettings(){
        //获取SharedPreference的实例对象，指定文件的名称和操作模式
        sharedPreferences = getSharedPreferences("location_mode",MODE_PRIVATE);
        //首次运行该程序，初始化时未生成对应文件，故往文件中存储数据以生成存储文件（首次默认普通态）
        File f = new File(
                "/data/data/com.example.location_baidumap/shared_prefs/location_mode.xml");
        if(!f.exists()) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("locMode","普通态");
            editor.commit();
        }
        //获取初始/上次设置的定位模式
        locationMode = sharedPreferences.getString("locMode","普通态");

    }

    /**
     * 与设置界面的交互
     */
    protected void settings() {
        settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * 功能一：切换地图类型
     */
    protected  void changeMapType(){
        //① MAP_TYPE_NORMAL  普通地图（包含3D地图）   ② MAP_TYPE_SATELLITE  卫星图    ③ MAP_TYPE_NONE  空白地图
        //在导航栏的单选按钮改变状态时触发事件（即为单选按钮组设置监听）
        mapTypeGroup = (RadioGroup) findViewById(R.id.map_type_group);
        mapTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup radioGroup,int checkedId){
                //根据checkedId获取到单选按钮组里面的具体单选按钮
                RadioButton radioButton = (RadioButton)findViewById(checkedId);
                //获取选中的单选按钮的值
                mapType = radioButton.getText().toString();
                setMapType(mapType);
            }
        });
    }

    /**
     * 设置地图类型
     * @param type 地图类型
     */
    protected void setMapType(String type){
        switch (type) {
            case "卫星图":
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                break;
            case "空白地图":
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NONE);
                break;
            default:
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        }
    }

    /**
     * 功能二：显示定位
     * ①显示定位 ②用户自定义设置定位
     */
    protected void showLocation(){
        //定位初始化--通过LocationClient发起定位
        mLocationClient = new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000); //周期性请求定位，1秒返回一次位置
        //参数设置locationClientOption
        mLocationClient.setLocOption(option);

        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);//开启地图定位图层
        mLocationClient.start();
    }

    /**
     * 设置LocationListener监听器获取定位数据
     * --通过继承抽象类BDAbstractListener并重写其onReceieveLocation方法来获取定位数据，并将其传给MapView。
     */
    public class MyLocationListener extends BDAbstractLocationListener {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null){
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);

            //自定义图标内容
            int circlrColor = MainActivity.this.getColor(R.color.green_loc);
            //定位模式--NORMAL（普通态）, FOLLOWING（跟随态）, COMPASS（罗盘态）
            MyLocationConfiguration.LocationMode mCurrentMode = setLocationMode(locationMode);
            BitmapDescriptor customMarker = BitmapDescriptorFactory.fromResource(R.drawable.loc_green);
            MyLocationConfiguration myLocationConfiguration = new MyLocationConfiguration(
                    mCurrentMode, //定位模式
                    true,      //开启方向
                    customMarker, //自定义定位图标
                    circlrColor,  //精度圈填充颜色
                    circlrColor   //精度圈边框颜色
            );
            mBaiduMap.setMyLocationConfiguration(myLocationConfiguration);

            //首次定位时，到达定位位置
            if (isFirstLoc) {
                isFirstLoc = false;
                //LatLng 存储经纬度坐标值的类，单位角度。这里赋值到缩放的中心点
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                //MapStatus.Builder 地图状态构造器
                MapStatus.Builder builder = new MapStatus.Builder();
                //设置缩放中心点以及缩放比例
                builder.target(latLng).zoom(19.0f);
                //给地图设置状态 [animateMapStatus--实现地图缩放级别改变的渐变动画效果]
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }
    }

    /**
     * 设置定位模式（单选框text→LocationMode）
     */
    protected MyLocationConfiguration.LocationMode setLocationMode(String mode) {
        switch(mode) {
            case "跟随态":
                return MyLocationConfiguration.LocationMode.FOLLOWING;
            case "罗盘态":
                return MyLocationConfiguration.LocationMode.COMPASS;
            default:
                return MyLocationConfiguration.LocationMode.NORMAL;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mMapView = null;
        if(mLocationClient != null) { //退出时销毁定位
            mLocationClient.stop();
        }
        mBaiduMap.setMyLocationEnabled(false); //关闭定位图层

    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

}
