package com.allin1.oneinbox;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static MainActivity visible;
    private static final int INK=0xff202b42, MUTED=0xff6f7a90, BLUE=0xff3031c5, BG=0xfff8f9fd;
    private static final String[] PACKAGES={"jp.naver.line.android","com.facebook.orca","com.whatsapp","com.whatsapp.w4b"};
    private final List<StatusBarNotification> notifications=new ArrayList<>();
    private LinearLayout root,content,chips;
    private EditText search;
    private String filter="ทั้งหมด",page="รวมข้อความ";

    static void refreshVisible(){if(visible!=null)visible.runOnUiThread(visible::reload);}
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.WHITE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);root=column();root.setBackgroundColor(BG);root.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(0,bars.top,0,bars.bottom);return insets;});setContentView(root);}
    @Override protected void onResume(){super.onResume();visible=this;reload();}
    @Override protected void onPause(){visible=null;super.onPause();}

    private void reload(){
        notifications.clear();MessageListener service=MessageListener.instance;
        if(service!=null&&hasAccess())try{
            for(StatusBarNotification item:service.getActiveNotifications())if(Arrays.asList(PACKAGES).contains(item.getPackageName())&&!TextUtils.isEmpty(item.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE)))notifications.add(item);
        }catch(RuntimeException e){Toast.makeText(this,"อ่านการแจ้งเตือนไม่สำเร็จ",Toast.LENGTH_SHORT).show();}
        notifications.sort(Comparator.comparingLong(StatusBarNotification::getPostTime).reversed());
        java.util.Set<String> seen=new java.util.HashSet<>();
        notifications.removeIf(item->{Notification n=item.getNotification();if((n.flags&Notification.FLAG_GROUP_SUMMARY)!=0)return true;
            return !seen.add(item.getPackageName()+"|"+n.extras.getCharSequence(Notification.EXTRA_TITLE,"")+"|"+n.extras.getCharSequence(Notification.EXTRA_TEXT,""));});
        render();
    }
    private void render(){
        String query=search==null?"":search.getText().toString();root.removeAllViews();header();
        ScrollView scroll=new ScrollView(this);content=column();content.setPadding(dp(18),dp(14),dp(18),dp(18));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(page.equals("รวมข้อความ"))inbox(query);else if(page.equals("แชทด่วน"))quickChats();else if(page.equals("แอปที่เชื่อมต่อ"))appsPage();else if(page.equals("disclosure"))disclosurePage();else settingsPage();bottomNav();
    }
    private void header(){
        LinearLayout bar=row();bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(18),dp(14),dp(18),dp(12));bar.setBackgroundColor(Color.WHITE);
        android.widget.ImageView logo=new android.widget.ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);bar.addView(logo,new LinearLayout.LayoutParams(dp(38),dp(38)));
        LinearLayout title=column();title.setPadding(dp(9),0,0,0);title.addView(text("OneNotify",18,INK,true));title.addView(text(page.equals("รวมข้อความ")?"Inbox Feed":page.equals("disclosure")?"การขอสิทธิ์":page,11,MUTED,false));bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        bar.addView(text(hasAccess()?"● พร้อมใช้งาน":"● ยังไม่เชื่อมต่อ",11,hasAccess()?0xff229669:0xffc85846,true));root.addView(bar);
    }
    private void inbox(String oldQuery){
        LinearLayout summary=column();summary.setPadding(dp(15),dp(12),dp(15),dp(12));summary.setBackground(round(0xffeef0ff,16));summary.addView(text("✦  ภาพรวมกล่องข้อความวันนี้",14,INK,true));summary.addView(text(notifications.size()+" การแจ้งเตือนที่ยังค้างอยู่ • อัปเดตจากเครื่องนี้",12,MUTED,false));content.addView(summary,mb(14));
        search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setHint("⌕  ค้นหาข้อความ หรือชื่อผู้ส่ง...");search.setPadding(dp(14),0,dp(14),0);search.setBackground(round(0xffebf0fa,13));search.setText(oldQuery);search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){renderList();}public void afterTextChanged(Editable e){}});content.addView(search,new LinearLayout.LayoutParams(-1,dp(46)));
        HorizontalScrollView strip=new HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(false);chips=row();chips.setPadding(0,dp(12),0,dp(10));strip.addView(chips);content.addView(strip);renderChips();renderList();
    }
    private void renderChips(){chips.removeAllViews();chip("ทั้งหมด",notifications.size());chip("LINE",count("LINE"));chip("Messenger",count("Messenger"));chip("WhatsApp",count("WhatsApp"));chip("WA Business",count("WA Business"));}
    private void chip(String name,int count){boolean active=filter.equals(name);TextView v=text(name+"  "+count,12,active?Color.WHITE:INK,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(13),dp(9),dp(13),dp(9));v.setBackground(round(active?BLUE:Color.WHITE,20));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=dp(7);chips.addView(v,p);v.setOnClickListener(w->{filter=name;renderChips();renderList();});}
    private void renderList(){
        if(content==null||!page.equals("รวมข้อความ"))return;while(content.getChildCount()>3)content.removeViewAt(3);
        if(!hasAccess()){notice("เปิดสิทธิ์เพื่อดูข้อความ","OneNotify ต้องได้รับสิทธิ์อ่านการแจ้งเตือนบนเครื่องนี้",true);return;}
        if(MessageListener.instance==null){notice("กำลังเชื่อมต่อ","รอสักครู่แล้วรายการจะอัปเดตอัตโนมัติ",false);return;}
        String query=search==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);List<StatusBarNotification> shown=new ArrayList<>();
        for(StatusBarNotification item:notifications){Notification n=item.getNotification();String source=source(item.getPackageName());String title=String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TITLE,""));String body=String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TEXT,""));if((filter.equals("ทั้งหมด")||filter.equals(source))&&(query.isEmpty()||(title+" "+body+" "+source).toLowerCase(Locale.ROOT).contains(query)))shown.add(item);}
        content.addView(text("ข้อความล่าสุด   "+shown.size()+" รายการ",16,INK,true),mb(8));if(shown.isEmpty()){notice("ไม่มีข้อความที่ตรงกับตัวกรอง","ลองเลือกแอปอื่นหรือเปลี่ยนคำค้นหา",false);return;}for(StatusBarNotification item:shown)messageCard(item);
    }
    private void messageCard(StatusBarNotification item){
        Notification n=item.getNotification();String source=source(item.getPackageName());int accent=source.equals("LINE")?0xff15b866:source.equals("Messenger")?0xff2185f7:0xff25b373;
        LinearLayout card=column();card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(round(Color.WHITE,16));content.addView(card,mb(9));
        LinearLayout top=row();top.setGravity(Gravity.CENTER_VERTICAL);TextView avatar=text(source.substring(0,1),16,Color.WHITE,true);avatar.setGravity(Gravity.CENTER);avatar.setBackground(round(accent,16));top.addView(avatar,new LinearLayout.LayoutParams(dp(36),dp(36)));
        LinearLayout who=column();who.setPadding(dp(10),0,0,0);who.addView(text(String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TITLE,"")),15,INK,true));who.addView(text(source+"  •  "+DateFormat.getTimeInstance(DateFormat.SHORT).format(item.getPostTime()),11,accent,false));top.addView(who,new LinearLayout.LayoutParams(0,-2,1));card.addView(top);
        CharSequence body=n.extras.getCharSequence(Notification.EXTRA_TEXT,"");if(!TextUtils.isEmpty(body)){TextView message=text(body.toString(),13,0xff4b5569,false);message.setPadding(dp(46),dp(8),0,dp(5));message.setMaxLines(3);message.setEllipsize(TextUtils.TruncateAt.END);card.addView(message);}
        TextView action=text("เปิดใน "+source+"   →",12,BLUE,true);action.setGravity(Gravity.RIGHT);card.addView(action);card.setOnClickListener(v->openOriginal(n.contentIntent));card.setClickable(true);card.setFocusable(true);
    }
    private void quickChats(){content.addView(text("แชทด่วน",23,INK,true),mb(8));notice("เปิดบทสนทนาจากกล่องข้อความ","เลือกการแจ้งเตือนในหน้า รวมข้อความ เพื่อเปิดแชตในแอปต้นทาง",false);action("ไปยังรวมข้อความ",()->{page="รวมข้อความ";render();});}
    private void appsPage(){content.addView(text("แอปที่เชื่อมต่อ",23,INK,true),mb(8));for(int i=0;i<PACKAGES.length;i++){String name=i==0?"LINE":i==1?"Messenger":i==2?"WhatsApp":"WhatsApp Business";boolean installed;try{getPackageManager().getPackageInfo(PACKAGES[i],0);installed=true;}catch(Exception e){installed=false;}LinearLayout card=column();card.setPadding(dp(15),dp(13),dp(15),dp(13));card.setBackground(round(Color.WHITE,15));card.addView(text(name,16,INK,true));card.addView(text(installed?"ติดตั้งบนเครื่องแล้ว":"ไม่พบแอปบนเครื่อง",12,installed?0xff229669:MUTED,false));content.addView(card,mb(9));}content.addView(text("รายการจะแสดงเมื่อมีการแจ้งเตือนและเปิดสิทธิ์ Notification access",12,MUTED,false));}
    private void settingsPage(){content.addView(text("ตั้งค่า",23,INK,true),mb(8));notice("สิทธิ์การแจ้งเตือน",hasAccess()?"เปิดใช้งานแล้ว":"ยังไม่ได้เปิดใช้งาน",false);action("เปิดการตั้งค่าสิทธิ์",()->showDisclosure());content.addView(text("OneNotify แสดงเฉพาะการแจ้งเตือนที่ยังค้างอยู่ ไม่เก็บข้อความหรือส่งข้อมูลออกจากเครื่อง",13,MUTED,false));}
    private void notice(String title,String detail,boolean enable){LinearLayout box=column();box.setPadding(dp(16),dp(17),dp(16),dp(17));box.setBackground(round(Color.WHITE,16));box.addView(text(title,16,INK,true));box.addView(text(detail,13,MUTED,false));content.addView(box,mb(12));if(enable)action("เปิดสิทธิ์อ่านการแจ้งเตือน",()->showDisclosure());}
    private void action(String title,Runnable task){TextView v=text(title,14,Color.WHITE,true);v.setGravity(Gravity.CENTER);v.setBackground(round(BLUE,13));content.addView(v,new LinearLayout.LayoutParams(-1,dp(46)));v.setOnClickListener(w->task.run());}
    private void bottomNav(){LinearLayout nav=row();nav.setBackgroundColor(Color.WHITE);nav.setPadding(dp(7),dp(7),dp(7),dp(7));String[] pages={"รวมข้อความ","แชทด่วน","แอปที่เชื่อมต่อ","ตั้งค่า"};String[] icons={"▣","▢","▦","⚙"};for(int i=0;i<pages.length;i++){String target=pages[i];LinearLayout item=column();item.setGravity(Gravity.CENTER);TextView ic=text(icons[i],21,page.equals(target)?BLUE:MUTED,true);ic.setGravity(Gravity.CENTER);item.addView(ic,new LinearLayout.LayoutParams(-1,-2));TextView lb=text(target,10,page.equals(target)?BLUE:MUTED,page.equals(target));lb.setGravity(Gravity.CENTER);lb.setSingleLine(true);item.addView(lb,new LinearLayout.LayoutParams(-1,-2));nav.addView(item,new LinearLayout.LayoutParams(0,dp(53),1));item.setOnClickListener(v->{page=target;search=null;render();});}root.addView(nav);}
    private void showDisclosure(){page="disclosure";render();}
    private void disclosurePage(){
        android.widget.ImageView pet=new android.widget.ImageView(this);pet.setImageResource(R.mipmap.ic_launcher);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(96),dp(96));pp.gravity=Gravity.CENTER_HORIZONTAL;pp.topMargin=dp(6);pp.bottomMargin=dp(14);content.addView(pet,pp);
        TextView title=text("ก่อนเปิดสิทธิ์อ่านการแจ้งเตือน",21,INK,true);title.setGravity(Gravity.CENTER);content.addView(title,mb(6));
        TextView lead=text("OneNotify ต้องใช้สิทธิ์ \"การเข้าถึงการแจ้งเตือน\" ของ Android เพื่อรวมข้อความจากหลายแอปไว้ในที่เดียว โปรดอ่านก่อนอนุญาต",13,MUTED,false);lead.setGravity(Gravity.CENTER);content.addView(lead,mb(16));
        disclosureItem("ข้อมูลที่อ่าน","ชื่อผู้ส่ง ข้อความ และเวลาที่ปรากฏในการแจ้งเตือนที่ยังค้างอยู่บนเครื่อง");
        disclosureItem("แอปที่แสดง","LINE, Messenger, WhatsApp และ WhatsApp Business เท่านั้น");
        disclosureItem("สิ่งที่ควรรู้","Android จะให้สิทธิ์อ่านการแจ้งเตือนจากทุกแอปในเครื่อง แต่ OneNotify จะไม่แสดงหรือใช้การแจ้งเตือนจากแอปอื่น");
        disclosureItem("ความเป็นส่วนตัว","ข้อมูลอยู่บนเครื่องนี้เท่านั้น ไม่บันทึกลงฐานข้อมูล ไม่ส่งขึ้นเซิร์ฟเวอร์ และไม่แชร์กับใคร");
        disclosureItem("ถอนสิทธิ์ได้ทุกเมื่อ","ไปที่ ตั้งค่า > การแจ้งเตือน > การเข้าถึงการแจ้งเตือน แล้วปิด OneNotify");
        action("ยอมรับและไปที่การตั้งค่า",()->{page="รวมข้อความ";startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));});
        TextView later=text("ไว้ทีหลัง",14,BLUE,true);later.setGravity(Gravity.CENTER);later.setPadding(0,dp(14),0,dp(14));later.setOnClickListener(v->{page="รวมข้อความ";render();});content.addView(later,new LinearLayout.LayoutParams(-1,-2));
    }
    private void disclosureItem(String head,String detail){LinearLayout box=column();box.setPadding(dp(16),dp(13),dp(16),dp(13));box.setBackground(round(Color.WHITE,14));box.addView(text(head,14,INK,true));box.addView(text(detail,13,MUTED,false));content.addView(box,mb(8));}
    private boolean hasAccess(){String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return enabled!=null&&enabled.contains(new ComponentName(this,MessageListener.class).flattenToString());}
    private int count(String source){int count=0;for(StatusBarNotification item:notifications)if(source(item.getPackageName()).equals(source))count++;return count;}
    private String source(String pkg){if(pkg.equals(PACKAGES[0]))return "LINE";if(pkg.equals(PACKAGES[1]))return "Messenger";if(pkg.equals(PACKAGES[3]))return "WA Business";return "WhatsApp";}
    private void openOriginal(PendingIntent intent){if(intent==null){Toast.makeText(this,"การแจ้งเตือนนี้ไม่มีลิงก์เปิดแชต",Toast.LENGTH_SHORT).show();return;}try{if(android.os.Build.VERSION.SDK_INT>=34){android.app.ActivityOptions o=android.app.ActivityOptions.makeBasic();o.setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);intent.send(this,0,null,null,null,null,o.toBundle());}else intent.send();}catch(PendingIntent.CanceledException e){Toast.makeText(this,"การแจ้งเตือนนี้หมดอายุแล้ว",Toast.LENGTH_SHORT).show();reload();}}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);return v;}
    private LinearLayout.LayoutParams mb(int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(bottom);return p;}
    private int dp(int value){return(int)(getResources().getDisplayMetrics().density*value+.5f);}
    private TextView text(String value,int sp,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(sp);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
}
