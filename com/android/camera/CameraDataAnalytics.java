package com.android.camera;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.v7.recyclerview.R;
import android.util.Xml.Encoding;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class CameraDataAnalytics {
    private static final String PREFERENCE_FILE_PATH = (CameraAppImpl.getAndroidContext().getPackageName() + "_data_analytics");
    private static CameraDataAnalytics sAnalytics;
    private static final HashMap<String, String> sTrackEventMap = new HashMap();
    private SharedPreferences mBackPreference;
    private SharedPreferences mFrontPreference;
    private SharedPreferences mPreference;

    private class Event {
        public String key;
        public String name;

        public Event(String strKey, String strName) {
            this.key = strKey;
            this.name = strName;
        }
    }

    private class Rule {
        public ArrayList<Event> eventList = new ArrayList();
        public String name;

        public Rule(String strName) {
            this.name = strName;
        }
    }

    private class RuleParser {
        private RuleParser() {
        }

        public ArrayList<Rule> parse(InputStream is) throws XmlPullParserException, IOException {
            ArrayList<Rule> ruleList = new ArrayList();
            Rule rule = null;
            Object event = null;
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(is, Encoding.UTF_8.name());
            for (int eventType = xpp.getEventType(); eventType != 1; eventType = xpp.next()) {
                String name;
                if (eventType == 2) {
                    name = xpp.getName();
                    if ("rule".equals(name)) {
                        rule = new Rule(xpp.getAttributeValue(null, "name"));
                    } else if ("event".equals(name)) {
                        event = new Event(xpp.getAttributeValue(null, "key"), xpp.getAttributeValue(null, "name"));
                    }
                } else if (eventType == 3) {
                    name = xpp.getName();
                    if ("rule".equals(name) && rule != null) {
                        ruleList.add(rule);
                    } else if ("event".equals(name)) {
                        int i;
                        if (rule != null) {
                            i = 1;
                        } else {
                            i = 0;
                        }
                        if (((event != null ? 1 : 0) & i) != 0) {
                            rule.eventList.add(event);
                        }
                    }
                }
            }
            return ruleList;
        }
    }

    static {
        sTrackEventMap.put("camera_picture_taken_key", "拍照次数");
        sTrackEventMap.put("video_recorded_key", "录像次数");
        sTrackEventMap.put("scroll_to_menu_key", "滑动到Menu次数");
        sTrackEventMap.put("scroll_to_effect_key", "滑动到特效次数");
        sTrackEventMap.put("pref_camera_panoramamode_key", "全景点击次数");
        sTrackEventMap.put("pref_camera_hdr_key", "HDR点击次数");
        sTrackEventMap.put("pref_camera_face_beauty_mode_key", "美颜模式点击次数");
        sTrackEventMap.put("pref_camera_burst_shooting_key", "连拍模式点击次数");
        sTrackEventMap.put("pref_camera_manual_mode_key", "手动模式点击次数");
        sTrackEventMap.put("pref_camera_hand_night_key", "夜景防抖点击次数");
        sTrackEventMap.put("pref_camera_ubifocus_key", "魔术对焦点击次数");
        sTrackEventMap.put("pref_camera_square_mode_key", "正方形点击次数");
        sTrackEventMap.put("pref_camera_scenemode_key", "场景点击次数");
        sTrackEventMap.put("pref_delay_capture_mode", "延时快门点击次数");
        sTrackEventMap.put("pref_camera_gradienter_key", "水平仪模式点击次数");
        sTrackEventMap.put("pref_camera_tilt_shift_mode", "移轴模式点击次数");
        sTrackEventMap.put("pref_camera_portrait_mode_key", "人像模式点击次数");
        sTrackEventMap.put("pref_camera_whitebalance_key", "白平衡点击次数");
        sTrackEventMap.put("pref_focus_position_key", "对焦点击次数");
        sTrackEventMap.put("pref_qc_camera_exposuretime_key", "快门时间点击次数");
        sTrackEventMap.put("pref_qc_camera_iso_key", "ISO点击次数");
        sTrackEventMap.put("manual_whitebalance_key", "手动白平衡点击次数");
        sTrackEventMap.put("pref_camera_shader_coloreffect_key", "滤镜点击次数");
        sTrackEventMap.put("pref_camera_exposure_key", "曝光环调节次数");
        sTrackEventMap.put("pref_camera_referenceline_key", "辅助线点击次数");
        sTrackEventMap.put("pref_settings", "设置点击次数");
        sTrackEventMap.put("capture_times_focus_view", "曝光环拍照次数");
        sTrackEventMap.put("capture_times_volume", "音量键拍照次数");
        sTrackEventMap.put("capture_times_shutter", "拍照键拍照次数");
        sTrackEventMap.put("capture_times_audio", "声控拍照次数");
        sTrackEventMap.put("capture_times_count_down", "倒计时拍照次数");
        sTrackEventMap.put("capture_times_count_down_3s", "倒计时3s次数");
        sTrackEventMap.put("capture_times_count_down_5s", "倒计时5s次数");
        sTrackEventMap.put("capture_times_count_down_10s", "倒计时10s次数");
        sTrackEventMap.put("capture_times_long_press", "长按屏幕拍照次数");
        sTrackEventMap.put("capture_times_t2t", "物体追踪拍照次数");
        sTrackEventMap.put("capture_times_finger", "指纹拍照次数");
        sTrackEventMap.put("record_times_finger", "指纹录像次数");
        sTrackEventMap.put("burst_times", "连拍次数");
        sTrackEventMap.put("capture_nums_normal_hdr", "标准HDR拍照张数");
        sTrackEventMap.put("capture_nums_live_hdr", "实时HDR拍照张数");
        sTrackEventMap.put("capture_nums_burst", "连拍张数");
        sTrackEventMap.put("capture_nums_beauty", "美颜张数");
        sTrackEventMap.put("capture_nums_hht", "夜间防抖张数");
        sTrackEventMap.put("capture_nums_ubfocus", "魔术对焦拍照张数");
        sTrackEventMap.put("capture_nums_manual", "手动拍照张数");
        sTrackEventMap.put("capture_nums_gradienter", "水平仪拍照张数");
        sTrackEventMap.put("capture_nums_tilt_shift_circle", "圆形移轴拍照张数");
        sTrackEventMap.put("capture_nums_tilt_shift_parallel", "平行移轴拍照张数");
        sTrackEventMap.put("capture_nums_video_capture", "录像时拍照张数");
        sTrackEventMap.put("capture_times_quick_snap", "街拍张数");
        sTrackEventMap.put("capture_nums_portrait", "人像模式拍照张数");
        sTrackEventMap.put("capture_nums_panorama", "全景拍照张数");
        sTrackEventMap.put("capture_times_size_16_9", "全屏张数");
        sTrackEventMap.put("capture_times_size_4_3", "半屏张数");
        sTrackEventMap.put("capture_times_size_1_1", "正方形张数");
        sTrackEventMap.put("t2t_times", "物体追踪次数");
        sTrackEventMap.put("zoom_gesture_times", "手势ZOOM次数");
        sTrackEventMap.put("zoom_volume_times", "音量键ZOOM次数");
        sTrackEventMap.put("pref_video_hdr_key", "HDR录像点击次数");
        sTrackEventMap.put("pref_video_speed_fast_key", "快动作录像点击次数");
        sTrackEventMap.put("pref_video_speed_slow_key", "慢动作录像点击次数");
        sTrackEventMap.put("video_fast_recording_times_key", "快动作录像次数");
        sTrackEventMap.put("video_slow_recording_times_key", "慢动作录像次数");
        sTrackEventMap.put("video_hdr_recording_times_key", "HDR录像次数");
        sTrackEventMap.put("video_pause_recording_times_key", "录像暂停次数");
        sTrackEventMap.put("video_torch_recording_times_key", "开闪光灯录像次数");
        sTrackEventMap.put("video_front_camera_recording_times_key", "前置录像次数");
        sTrackEventMap.put("video_quality_4k_recording_times_key", "超高清录像次数");
        sTrackEventMap.put("video_quality_1080_recording_times_key", "全高清录像次数");
        sTrackEventMap.put("video_quality_720_recording_times_key", "高清录像次数");
        sTrackEventMap.put("video_quality_480_recording_times_key", "标清录像次数");
        sTrackEventMap.put("open_camera_times_key", "相机开启持续时间");
        sTrackEventMap.put("open_camera_fail_key", "相机无法连接次数");
        sTrackEventMap.put("picture_with_location_key", "记录位置信息图片张数");
        sTrackEventMap.put("video_with_location_key", "记录位置信息视频次数");
        sTrackEventMap.put("picture_without_location_key", "缺少位置信息图片张数");
        sTrackEventMap.put("video_without_location_key", "缺少位置信息视频次数");
        sTrackEventMap.put("front_camera_picture_taken_key", "自拍张数");
        sTrackEventMap.put("back_camera_picture_taken_key", "后置张数");
        sTrackEventMap.put("effect_instagram_clarendon_picture_taken_key", "小清新滤镜张数");
        sTrackEventMap.put("effect_instagram_crema_picture_taken_key", "秋色滤镜张数");
        sTrackEventMap.put("effect_vsco_f2_picture_taken_key", "典雅滤镜张数");
        sTrackEventMap.put("effect_vsco_a4_picture_taken_key", "唯美滤镜张数");
        sTrackEventMap.put("effect_blackwhite_picture_taken_key", "褪色滤镜张数");
        sTrackEventMap.put("effect_instagram_rise_picture_taken_key", "LOMO滤镜张数");
        sTrackEventMap.put("effect_instagram_hudson_picture_taken_key", "蓝调滤镜张数");
        sTrackEventMap.put("effect_big_face_picture_taken_key", "大脸猫滤镜张数");
        sTrackEventMap.put("effect_small_face_picture_taken_key", "外星人滤镜张数");
        sTrackEventMap.put("effect_long_face_picture_taken_key", "拉伸滤镜张数");
        sTrackEventMap.put("effect_light_tunnel_picture_taken_key", "光隧道滤镜张数");
        sTrackEventMap.put("effect_gray_picture_taken_key", "黑白滤镜张数");
        sTrackEventMap.put("effect_vivid_picture_taken_key", "生动滤镜张数");
        sTrackEventMap.put("effect_yesteryear_picture_taken_key", "往昔滤镜张数");
        sTrackEventMap.put("effect_gaussian_picture_taken_key", "背景虚化滤镜张数");
        sTrackEventMap.put("effect_fisheye_picture_taken_key", "鱼眼滤镜张数");
        sTrackEventMap.put("effect_mosaic_picture_taken_key", "马赛克滤镜张数");
        sTrackEventMap.put("effect_brannan_picture_taken_key", "晨光滤镜张数");
        sTrackEventMap.put("effect_seventy_picture_taken_key", "年代70滤镜张数");
        sTrackEventMap.put("effect_jstyle_picture_taken_key", "日系滤镜张数");
        sTrackEventMap.put("effect_earlybird_picture_taken_key", "秋色滤镜张数");
        sTrackEventMap.put("effect_nashvile_picture_taken_key", "靛青滤镜张数");
        sTrackEventMap.put("effect_mirror_picture_taken_key", "镜面滤镜张数");
        sTrackEventMap.put("effect_sketch_picture_taken_key", "素描滤镜张数");
        sTrackEventMap.put("qrcode_detected_times_key", "二维码识别次数");
        sTrackEventMap.put("time_watermark_taken_key", "时间水印照片张数");
        sTrackEventMap.put("faceinfo_watermark_taken_key", "人脸信息水印照片张数");
        sTrackEventMap.put("dual_watermark_taken_key", "双摄水印照片张数");
        sTrackEventMap.put("panorama_capture_left_start", "全景左起张数");
        sTrackEventMap.put("panorama_capture_right_start", "全景右起张数");
        sTrackEventMap.put("panorama_capture_2_times_left_start", "全景2次左起张数");
        sTrackEventMap.put("panorama_capture_2_times_right_start", "全景2次右起张数");
        sTrackEventMap.put("panorama_capture_2_times_random_start", "全景2次随机起张数");
        sTrackEventMap.put("launch_normal_times_key", "常规启动次数");
        sTrackEventMap.put("launch_keyguard_times_key", "锁屏启动次数");
        sTrackEventMap.put("launch_volume_key_times_key", "双击音量启动次数");
        sTrackEventMap.put("launch_snap_times_key", "街拍启动次数");
        sTrackEventMap.put("launch_capture_intent_times_key", "拍照意图启动次数");
        sTrackEventMap.put("launch_video_intent_times_key", "录像意图启动次数");
        sTrackEventMap.put("touch_focus_focus_view_capture_times_key", "触摸对焦后对焦环拍照次数");
        sTrackEventMap.put("touch_focus_shutter_capture_times_key", "触摸对焦后拍照按钮拍照次数");
        sTrackEventMap.put("touch_focus_volume_capture_times_key", "触摸对焦后音量键拍照次数");
        sTrackEventMap.put("ev_adjust_keep_time_key", "ev调节拍照后固定次数");
        sTrackEventMap.put("ev_adjust_recom_times_key", "ev调节拍照后再调节次数");
        sTrackEventMap.put("zoom_times_key", "zoom调节次数");
        sTrackEventMap.put("proximity_lock_volume_times_key", "防误触音量键次数");
        sTrackEventMap.put("proximity_lock_snap_times_key", "防误触街拍次数");
        sTrackEventMap.put("proximity_lock_keyguard_times_key", "防误触锁屏进入次数");
        sTrackEventMap.put("proximity_lock_keyguard_unlock_times_key", "防误触锁屏自动解除次数");
        sTrackEventMap.put("proximity_lock_keyguard_timeout_times_key", "防误触锁屏超时退出次数");
        sTrackEventMap.put("proximity_lock_keyguard_dismiss_times_key", "防误触锁屏手动解除次数");
        sTrackEventMap.put("proximity_lock_sensor_delay_times_key", "防误触模式sensor延迟次数");
    }

    public static CameraDataAnalytics instance() {
        if (sAnalytics == null) {
            sAnalytics = new CameraDataAnalytics();
        }
        return sAnalytics;
    }

    private CameraDataAnalytics() {
    }

    public void openPreference(int cameraId) {
        if (cameraId < 0) {
            this.mPreference = null;
        } else if (cameraId == CameraHolder.instance().getFrontCameraId()) {
            if (this.mFrontPreference == null) {
                this.mFrontPreference = CameraAppImpl.getAndroidContext().getSharedPreferences(PREFERENCE_FILE_PATH + "_front", 0);
            }
            this.mPreference = this.mFrontPreference;
        } else if (cameraId == CameraHolder.instance().getBackCameraId()) {
            if (this.mBackPreference == null) {
                this.mBackPreference = CameraAppImpl.getAndroidContext().getSharedPreferences(PREFERENCE_FILE_PATH + "_back", 0);
            }
            this.mPreference = this.mBackPreference;
        }
    }

    public void uploadToServer() {
        uploadToServer("FrontCamera", this.mFrontPreference);
        uploadToServer("BackCamera", this.mBackPreference);
    }

    private void uploadToServer(String category, SharedPreferences preference) {
        if (preference != null) {
            long lastTime = System.currentTimeMillis() - preference.getLong("open_camera_times_key", 0);
            Editor editor;
            if (lastTime > 86400000 || lastTime < 2000) {
                editor = preference.edit();
                editor.clear();
                editor.apply();
                return;
            }
            HashMap<String, ArrayList<String>> eventKeyToRulesMap = getEventKeyToRulesMap();
            try {
                for (String key : sTrackEventMap.keySet()) {
                    long value = preference.getLong(key, 0);
                    if (value != 0) {
                        if (key.equals("open_camera_times_key")) {
                            MiStatInterface.recordCalculateEvent(category, (String) sTrackEventMap.get(key), lastTime / 1000);
                        } else if (eventKeyToRulesMap.containsKey(key)) {
                            for (String ruleName : (ArrayList) eventKeyToRulesMap.get(key)) {
                                HashMap<String, String> params = new HashMap();
                                params.put("name", (String) sTrackEventMap.get(key));
                                MiStatInterface.recordCalculateEvent(category, ruleName, value, params);
                            }
                        } else {
                            MiStatInterface.recordCalculateEvent(category, (String) sTrackEventMap.get(key), value);
                        }
                    }
                }
                editor = preference.edit();
                editor.clear();
                editor.apply();
            } finally {
                eventKeyToRulesMap.clear();
            }
        }
    }

    public void trackEvent(String key) {
        trackEvent(key, 1);
    }

    public void trackEventTime(String key) {
        trackEvent(key, System.currentTimeMillis());
    }

    public void trackEvent(String key, long newValue) {
        if (key != null) {
            String trackEvent = (String) sTrackEventMap.get(key);
            if (!(this.mPreference == null || trackEvent == null)) {
                Editor editor = this.mPreference.edit();
                editor.putLong(key, newValue + this.mPreference.getLong(key, 0));
                editor.apply();
            }
        }
    }

    private HashMap<String, ArrayList<String>> getEventKeyToRulesMap() {
        ArrayList<Rule> ruleList = null;
        try {
            ruleList = new RuleParser().parse(CameraAppImpl.getAndroidContext().getResources().openRawResource(R.raw.statistic_rules));
        } catch (XmlPullParserException e) {
            Log.w("CameraDataAnalytics", "XmlPullParserException", e);
        } catch (IOException e2) {
            Log.w("CameraDataAnalytics", "IOException", e2);
        }
        return invertRule(ruleList);
    }

    private HashMap<String, ArrayList<String>> invertRule(ArrayList<Rule> ruleList) {
        HashMap<String, ArrayList<String>> eventKeyRulesMap = new HashMap();
        if (ruleList != null) {
            Iterator<Rule> ruleIter = ruleList.iterator();
            while (ruleIter.hasNext()) {
                Rule rule = (Rule) ruleIter.next();
                if (!(rule.name == null || rule.eventList == null)) {
                    Iterator<Event> eventIter = rule.eventList.iterator();
                    while (eventIter.hasNext()) {
                        Event event = (Event) eventIter.next();
                        if (event.key != null) {
                            String expectedName = (String) sTrackEventMap.get(event.key);
                            if (expectedName == null || !expectedName.equals(event.name)) {
                                Log.d("CameraDataAnalytics", "NOT MATCH: key=" + event.key + " name=" + event.name + " expectedName=" + expectedName);
                            }
                            ArrayList<String> ruleNameList = (ArrayList) eventKeyRulesMap.get(event.key);
                            if (ruleNameList == null) {
                                ruleNameList = new ArrayList();
                                ruleNameList.add(rule.name);
                                eventKeyRulesMap.put(event.key, ruleNameList);
                            } else if (!ruleNameList.contains(rule.name)) {
                                ruleNameList.add(rule.name);
                            }
                        }
                    }
                }
            }
        }
        return eventKeyRulesMap;
    }
}
