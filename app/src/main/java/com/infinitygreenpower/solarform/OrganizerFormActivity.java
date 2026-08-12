package com.infinitygreenpower.solarform;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class OrganizerFormActivity extends Activity {
    private final int GREEN=Color.rgb(41,139,82), DARK_GREEN=Color.rgb(22,92,57), NAVY=Color.rgb(25,62,96), BG=Color.rgb(246,249,247), TEXT=Color.rgb(28,39,46), MUTED=Color.rgb(105,120,126), BORDER=Color.rgb(218,229,222), SOFT=Color.rgb(239,248,242);
    private final int PICK_IMAGES=7101;
    private LinearLayout root, itemList, photoList;
    private EditText clientName, phone, location, date, capacity, notes, organizer;
    private Spinner systemType, phase;
    private CheckBox installation, transport;
    private final ArrayList<Product> catalog=new ArrayList<>();
    private final ArrayList<Item> items=new ArrayList<>();
    private final ArrayList<Uri> photos=new ArrayList<>();
    private String editingId=null;

    static class Product { String category, model, spec; Product(String a,String b,String c){category=a;model=b;spec=c;} String display(){return category+" — "+model+(spec.isEmpty()?"":" — "+spec);} String search(){return display().toLowerCase(Locale.ROOT);} }
    static class Item { String name, model, spec, qty, price; Item(String a,String b,String c,String d,String e){name=a;model=b;spec=c;qty=d;price=e;} }

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(DARK_GREEN);seed();showHome();}

    private void seed(){
        catalog.add(new Product("الإنفرتر","Roypow RS6500 Hybrid","6.5 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid Single Phase","6 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid Single Phase","8 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid Single Phase","12 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid Single Phase","16 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid Three Phase","12 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid Three Phase","16 kW"));
        catalog.add(new Product("الألواح الشمسية","Huasun HJT","610 W"));
        catalog.add(new Product("الألواح الشمسية","Huasun HJT","615 W"));
        catalog.add(new Product("الألواح الشمسية","Huasun HJT","620 W"));
        catalog.add(new Product("البطارية","Roypow LiFePO4","16 kWh"));
        catalog.add(new Product("البطارية","Deye High Voltage Battery Rack","HV"));
        catalog.add(new Product("BMS","Deye High Voltage BMS","HV"));
        catalog.add(new Product("القواعد","هيكل تثبيت ألواح شمسية","حسب الموقع"));
        catalog.add(new Product("كيبل DC","Solar DC Cable","حدد القياس"));
        catalog.add(new Product("كيبل AC","AC Cable","حدد القياس"));
        catalog.add(new Product("كيبل البطاريات","Battery Cable","حدد القياس"));
        catalog.add(new Product("صندوق حماية","Protection / Combiner Box",""));
        catalog.add(new Product("قاطع DC","DC Breaker",""));
        catalog.add(new Product("قاطع AC","AC Breaker",""));
        catalog.add(new Product("حماية DC","DC SPD",""));
        catalog.add(new Product("حماية AC","AC SPD",""));
        catalog.add(new Product("موصلات","MC4 Connectors",""));
        catalog.add(new Product("تأريض","Earthing System",""));
        catalog.add(new Product("مسار كيبل","Cable Tray / PVC",""));
    }

    private void showHome(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(dp(18),dp(18),dp(18),dp(28));scroll.addView(root,new ScrollView.LayoutParams(-1,-2));

        LinearLayout hero=card();hero.setPadding(dp(20),dp(20),dp(20),dp(20));
        TextView brand=txt("INFINITY GREEN POWER",15,true,GREEN);brand.setGravity(Gravity.RIGHT);hero.addView(brand);
        TextView title=txt("Organizer Quotation Form",27,true,TEXT);title.setGravity(Gravity.RIGHT);hero.addView(title);
        TextView ar=txt("استمارة بسيطة لتجميع معلومات عرض السعر",15,false,MUTED);ar.setGravity(Gravity.RIGHT);ar.setPadding(0,dp(4),0,0);hero.addView(ar);
        root.addView(hero);

        Button newForm=primary("+ استمارة جديدة");LinearLayout.LayoutParams nlp=(LinearLayout.LayoutParams)newForm.getLayoutParams();nlp.topMargin=dp(16);newForm.setLayoutParams(nlp);root.addView(newForm);
        newForm.setOnClickListener(v->startNew());

        TextView savedTitle=txt("الاستمارات المحفوظة",18,true,TEXT);savedTitle.setPadding(0,dp(24),0,dp(10));root.addView(savedTitle);
        ArrayList<JSONObject> saved=loadSaved();
        if(saved.isEmpty()) root.addView(empty("لا توجد استمارات محفوظة","أنشئ استمارة جديدة، احفظها ثم أرسل ملف PDF إليك."));
        else for(JSONObject o:saved) root.addView(savedCard(o));
        setContentView(scroll);
    }

    private View savedCard(JSONObject o){
        LinearLayout c=card();LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)c.getLayoutParams();lp.topMargin=dp(10);c.setLayoutParams(lp);
        String name=o.optString("client","بدون اسم"), dt=o.optString("date",""), loc=o.optString("location","");
        TextView n=txt(name,17,true,TEXT);c.addView(n);TextView meta=txt((loc.isEmpty()?"":loc+" • ")+dt,12,false,MUTED);c.addView(meta);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,-2);rlp.topMargin=dp(10);row.setLayoutParams(rlp);
        Button open=smallBtn("فتح",GREEN,Color.WHITE);Button pdf=smallBtn("PDF",NAVY,Color.WHITE);Button del=smallBtn("حذف",Color.WHITE,Color.rgb(180,45,45));((GradientDrawable)del.getBackground()).setStroke(dp(1),Color.rgb(238,200,200));row.addView(open);row.addView(pdf);row.addView(del);c.addView(row);
        open.setOnClickListener(v->{loadIntoEditor(o);});
        pdf.setOnClickListener(v->{loadDataOnly(o);createPdfAndShare();showHome();});
        del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("حذف هذه الاستمارة؟").setPositiveButton("حذف",(d,w)->{deleteSaved(o.optString("id"));showHome();}).setNegativeButton("إلغاء",null).show());
        return c;
    }

    private void startNew(){editingId=null;items.clear();photos.clear();showEditor();}

    private void showEditor(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(dp(16),dp(14),dp(16),dp(30));scroll.addView(root,new ScrollView.LayoutParams(-1,-2));

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=txt(editingId==null?"استمارة جديدة":"تعديل الاستمارة",22,true,TEXT);title.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));top.addView(title);
        TextView close=txt("إغلاق",13,true,GREEN);close.setPadding(dp(12),dp(8),dp(12),dp(8));close.setBackground(round(SOFT,14,SOFT,0));close.setOnClickListener(v->showHome());top.addView(close);root.addView(top);
        TextView subtitle=txt("املأ المطلوب فقط، ثم احفظ أو أنشئ PDF وأرسله.",13,false,MUTED);subtitle.setPadding(0,dp(4),0,dp(10));root.addView(subtitle);

        LinearLayout c1=section("1","معلومات العميل");
        clientName=field(c1,"اسم العميل","اسم العميل",false,InputType.TYPE_CLASS_TEXT);
        phone=field(c1,"رقم الهاتف","07xx xxx xxxx",false,InputType.TYPE_CLASS_PHONE);
        location=field(c1,"الموقع","المدينة / المنطقة",false,InputType.TYPE_CLASS_TEXT);
        date=field(c1,"التاريخ","YYYY/MM/DD",false,InputType.TYPE_CLASS_TEXT);date.setText(new SimpleDateFormat("yyyy/MM/dd",Locale.US).format(new Date()));

        LinearLayout c2=section("2","معلومات المنظومة");
        systemType=spinner(c2,"نوع المنظومة",new String[]{"اختر","هجين","أون كريد","أوف كريد"});
        capacity=field(c2,"السعة المطلوبة (kW)","مثال: 8",false,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        phase=spinner(c2,"الطور",new String[]{"اختر","أحادي","ثلاثي"});

        LinearLayout c3=section("3","المواد والأسعار");
        TextView h=txt("اختر المواد من القائمة أو أضف مادة مخصصة. السعر اختياري.",12,false,MUTED);h.setPadding(0,0,0,dp(8));c3.addView(h);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);Button pick=smallBtn("اختيار مادة",GREEN,Color.WHITE);Button custom=smallBtn("+ مادة مخصصة",Color.WHITE,GREEN);((GradientDrawable)custom.getBackground()).setStroke(dp(1),BORDER);actions.addView(pick);actions.addView(custom);c3.addView(actions);
        itemList=new LinearLayout(this);itemList.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(-1,-2);ilp.topMargin=dp(10);itemList.setLayoutParams(ilp);c3.addView(itemList);renderItems();pick.setOnClickListener(v->showCatalog());custom.setOnClickListener(v->showItemDialog(new Item("","","","1",""),true,-1));

        LinearLayout c4=section("4","صور الموقع");
        Button photo=outlineBtn("+ إرفاق صور من الهاتف");c4.addView(photo);photoList=new LinearLayout(this);photoList.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(-1,-2);plp.topMargin=dp(8);photoList.setLayoutParams(plp);c4.addView(photoList);renderPhotos();photo.setOnClickListener(v->pickImages());

        LinearLayout c5=section("5","معلومات إضافية");
        LinearLayout checks=new LinearLayout(this);checks.setOrientation(LinearLayout.HORIZONTAL);checks.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);installation=new CheckBox(this);installation.setText("يشمل التركيب");transport=new CheckBox(this);transport.setText("يشمل النقل");checks.addView(installation);checks.addView(transport);c5.addView(checks);
        organizer=field(c5,"اسم منظم الكشف","اسم المنظم",false,InputType.TYPE_CLASS_TEXT);
        notes=field(c5,"ملاحظات","ملاحظات مختصرة إن وجدت",true,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        Button pdf=primary("إنشاء PDF ومشاركته");LinearLayout.LayoutParams pp=(LinearLayout.LayoutParams)pdf.getLayoutParams();pp.topMargin=dp(16);pdf.setLayoutParams(pp);root.addView(pdf);
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.HORIZONTAL);bottom.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,-2);blp.topMargin=dp(8);bottom.setLayoutParams(blp);Button save=smallBtn("حفظ الاستمارة",NAVY,Color.WHITE);Button home=smallBtn("العودة للرئيسية",Color.WHITE,TEXT);((GradientDrawable)home.getBackground()).setStroke(dp(1),BORDER);bottom.addView(save);bottom.addView(home);root.addView(bottom);
        save.setOnClickListener(v->{saveCurrent();Toast.makeText(this,"تم حفظ الاستمارة",Toast.LENGTH_SHORT).show();showHome();});
        pdf.setOnClickListener(v->{saveCurrent();createPdfAndShare();});home.setOnClickListener(v->showHome());
        setContentView(scroll);
    }

    private LinearLayout section(String n,String title){
        LinearLayout c=card();LinearLayout.LayoutParams cp=(LinearLayout.LayoutParams)c.getLayoutParams();cp.topMargin=dp(12);c.setLayoutParams(cp);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge=txt(n,12,true,Color.WHITE);badge.setGravity(Gravity.CENTER);badge.setBackground(round(GREEN,12,GREEN,0));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(34),dp(28));bp.leftMargin=dp(10);badge.setLayoutParams(bp);head.addView(badge);head.addView(txt(title,17,true,TEXT),new LinearLayout.LayoutParams(0,-2,1));c.addView(head);
        View line=new View(this);line.setBackgroundColor(BORDER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(10);lp.bottomMargin=dp(10);line.setLayoutParams(lp);c.addView(line);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);c.addView(body);root.addView(c);return body;
    }

    private EditText field(LinearLayout p,String label,String hint,boolean multi,int input){TextView l=txt(label,13,true,TEXT);LinearLayout.LayoutParams ll=new LinearLayout.LayoutParams(-1,-2);ll.bottomMargin=dp(5);l.setLayoutParams(ll);p.addView(l);EditText e=new EditText(this);e.setHint(hint);e.setTextSize(15);e.setTextColor(TEXT);e.setHintTextColor(Color.rgb(150,165,160));e.setTextDirection(View.TEXT_DIRECTION_RTL);e.setGravity(multi?Gravity.TOP|Gravity.RIGHT:Gravity.CENTER_VERTICAL|Gravity.RIGHT);e.setBackground(round(Color.WHITE,15,BORDER,1));e.setPadding(dp(13),dp(10),dp(13),dp(10));e.setInputType(input);if(multi)e.setSingleLine(false);else e.setSingleLine(true);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,multi?dp(90):dp(50));ep.bottomMargin=dp(10);e.setLayoutParams(ep);p.addView(e);return e;}
    private Spinner spinner(LinearLayout p,String label,String[] values){TextView l=txt(label,13,true,TEXT);LinearLayout.LayoutParams ll=new LinearLayout.LayoutParams(-1,-2);ll.bottomMargin=dp(5);l.setLayoutParams(ll);p.addView(l);Spinner s=new Spinner(this,Spinner.MODE_DROPDOWN);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);s.setBackground(round(Color.WHITE,15,BORDER,1));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(50));sp.bottomMargin=dp(10);s.setLayoutParams(sp);p.addView(s);return s;}

    private void showCatalog(){
        Dialog d=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(14),dp(14),dp(14));box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView t=txt("اختيار مادة",20,true,TEXT);box.addView(t);EditText q=new EditText(this);q.setHint("بحث: Deye, Huasun, Roypow, كيبل...");q.setBackground(round(Color.WHITE,14,BORDER,1));q.setPadding(dp(12),dp(9),dp(12),dp(9));q.setTextDirection(View.TEXT_DIRECTION_RTL);q.setGravity(Gravity.RIGHT);LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-1,dp(48));qp.topMargin=dp(8);box.addView(q,qp);ListView list=new ListView(this);LinearLayout.LayoutParams lvp=new LinearLayout.LayoutParams(-1,dp(430));lvp.topMargin=dp(8);box.addView(list,lvp);
        ArrayList<Product> filtered=new ArrayList<>(catalog);ArrayList<String> names=new ArrayList<>();for(Product p:filtered)names.add(p.display());ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,names);list.setAdapter(ad);
        q.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){String x=s.toString().toLowerCase(Locale.ROOT);filtered.clear();names.clear();for(Product p:catalog)if(x.isEmpty()||p.search().contains(x)){filtered.add(p);names.add(p.display());}ad.notifyDataSetChanged();}});
        list.setOnItemClickListener((p,v,pos,id)->{Product x=filtered.get(pos);d.dismiss();showItemDialog(new Item(x.category,x.model,x.spec,"1",""),false,-1);});d.setContentView(box);d.show();Window w=d.getWindow();if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94),WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void showItemDialog(Item base,boolean nameEditable,int editIndex){
        LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(16),dp(8),dp(16),0);b.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText name=dialogField(b,"المادة","اسم المادة");name.setText(base.name);name.setEnabled(nameEditable);EditText model=dialogField(b,"الماركة / الموديل","الماركة أو الموديل");model.setText(base.model);EditText spec=dialogField(b,"السعة / القياس","مثال: 620W أو 16kWh");spec.setText(base.spec);EditText qty=dialogField(b,"الكمية","1");qty.setText(base.qty.isEmpty()?"1":base.qty);EditText price=dialogField(b,"السعر","اختياري");price.setText(base.price);
        new AlertDialog.Builder(this).setTitle(editIndex>=0?"تعديل المادة":"إضافة المادة").setView(b).setPositiveButton(editIndex>=0?"حفظ":"إضافة",(d,w)->{Item it=new Item(text(name),text(model),text(spec),text(qty),text(price));if(it.name.isEmpty())it.name="مادة أخرى";if(editIndex>=0)items.set(editIndex,it);else items.add(it);renderItems();}).setNegativeButton("إلغاء",null).show();
    }
    private EditText dialogField(LinearLayout p,String label,String hint){TextView l=txt(label,13,true,TEXT);p.addView(l);EditText e=new EditText(this);e.setHint(hint);e.setGravity(Gravity.RIGHT);e.setTextDirection(View.TEXT_DIRECTION_RTL);e.setBackground(round(Color.WHITE,14,BORDER,1));e.setPadding(dp(12),dp(8),dp(12),dp(8));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(46));lp.bottomMargin=dp(8);e.setLayoutParams(lp);p.addView(e);return e;}

    private void renderItems(){if(itemList==null)return;itemList.removeAllViews();if(items.isEmpty()){itemList.addView(empty("لا توجد مواد","اضغط اختيار مادة أو مادة مخصصة."));return;}for(int i=0;i<items.size();i++){final int idx=i;Item it=items.get(i);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(10),dp(12),dp(10));c.setBackground(round(Color.rgb(251,253,252),15,BORDER,1));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(8);c.setLayoutParams(cp);LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);TextView n=txt(it.name,15,true,TEXT);n.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));top.addView(n);TextView edit=tag("تعديل",GREEN,SOFT);TextView del=tag("حذف",Color.rgb(180,45,45),Color.rgb(255,241,241));top.addView(edit);top.addView(del);c.addView(top);String details=(it.model.isEmpty()?"":it.model)+(!it.spec.isEmpty()?" • "+it.spec:"")+" • الكمية: "+safe(it.qty)+(!it.price.isEmpty()?" • السعر: "+it.price:"");TextView dt=txt(details,12,false,MUTED);dt.setPadding(0,dp(5),0,0);c.addView(dt);edit.setOnClickListener(v->showItemDialog(new Item(it.name,it.model,it.spec,it.qty,it.price),true,idx));del.setOnClickListener(v->{items.remove(idx);renderItems();});itemList.addView(c);}}

    private void pickImages(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,PICK_IMAGES);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==PICK_IMAGES&&res==RESULT_OK&&data!=null){if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)addPhoto(data.getClipData().getItemAt(i).getUri(),data.getFlags());}else if(data.getData()!=null)addPhoto(data.getData(),data.getFlags());renderPhotos();}}
    private void addPhoto(Uri u,int f){try{getContentResolver().takePersistableUriPermission(u,f&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}if(!photos.contains(u))photos.add(u);}
    private void renderPhotos(){if(photoList==null)return;photoList.removeAllViews();if(photos.isEmpty()){photoList.addView(empty("لا توجد صور مرفقة","يمكنك إرفاق صور الموقع أو المنظومة الحالية."));return;}for(int i=0;i<photos.size();i++){final int idx=i;LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);r.setPadding(dp(10),dp(8),dp(10),dp(8));r.setBackground(round(Color.WHITE,14,BORDER,1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(6);r.setLayoutParams(rp);TextView n=txt("📷 "+displayName(photos.get(i)),12,false,TEXT);n.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));r.addView(n);TextView d=tag("حذف",Color.rgb(180,45,45),Color.rgb(255,241,241));r.addView(d);d.setOnClickListener(v->{photos.remove(idx);renderPhotos();});photoList.addView(r);}}

    private void saveCurrent(){try{JSONObject o=currentJson();ArrayList<JSONObject> list=loadSaved();boolean replaced=false;for(int i=0;i<list.size();i++)if(list.get(i).optString("id").equals(o.optString("id"))){list.set(i,o);replaced=true;break;}if(!replaced)list.add(0,o);JSONArray a=new JSONArray();for(JSONObject x:list)a.put(x);getSharedPreferences("organizer_forms",0).edit().putString("forms",a.toString()).apply();editingId=o.optString("id");}catch(Exception e){Toast.makeText(this,"تعذر الحفظ",Toast.LENGTH_SHORT).show();}}
    private JSONObject currentJson() throws Exception{JSONObject o=new JSONObject();if(editingId==null)editingId=String.valueOf(System.currentTimeMillis());o.put("id",editingId);o.put("client",text(clientName));o.put("phone",text(phone));o.put("location",text(location));o.put("date",text(date));o.put("system",spin(systemType));o.put("capacity",text(capacity));o.put("phase",spin(phase));o.put("installation",installation.isChecked());o.put("transport",transport.isChecked());o.put("organizer",text(organizer));o.put("notes",text(notes));JSONArray ia=new JSONArray();for(Item it:items){JSONObject x=new JSONObject();x.put("name",it.name);x.put("model",it.model);x.put("spec",it.spec);x.put("qty",it.qty);x.put("price",it.price);ia.put(x);}o.put("items",ia);JSONArray pa=new JSONArray();for(Uri u:photos)pa.put(u.toString());o.put("photos",pa);return o;}
    private ArrayList<JSONObject> loadSaved(){ArrayList<JSONObject> out=new ArrayList<>();try{JSONArray a=new JSONArray(getSharedPreferences("organizer_forms",0).getString("forms","[]"));for(int i=0;i<a.length();i++)out.add(a.getJSONObject(i));}catch(Exception ignored){}return out;}
    private void deleteSaved(String id){ArrayList<JSONObject> l=loadSaved();JSONArray a=new JSONArray();for(JSONObject o:l)if(!o.optString("id").equals(id))a.put(o);getSharedPreferences("organizer_forms",0).edit().putString("forms",a.toString()).apply();}
    private void loadIntoEditor(JSONObject o){items.clear();photos.clear();editingId=o.optString("id");showEditor();applyJson(o);}
    private void loadDataOnly(JSONObject o){items.clear();photos.clear();editingId=o.optString("id");showEditor();applyJson(o);}
    private void applyJson(JSONObject o){clientName.setText(o.optString("client"));phone.setText(o.optString("phone"));location.setText(o.optString("location"));date.setText(o.optString("date"));capacity.setText(o.optString("capacity"));organizer.setText(o.optString("organizer"));notes.setText(o.optString("notes"));select(systemType,o.optString("system"));select(phase,o.optString("phase"));installation.setChecked(o.optBoolean("installation"));transport.setChecked(o.optBoolean("transport"));items.clear();JSONArray ia=o.optJSONArray("items");if(ia!=null)for(int i=0;i<ia.length();i++){JSONObject x=ia.optJSONObject(i);items.add(new Item(x.optString("name"),x.optString("model"),x.optString("spec"),x.optString("qty"),x.optString("price")));}photos.clear();JSONArray pa=o.optJSONArray("photos");if(pa!=null)for(int i=0;i<pa.length();i++)photos.add(Uri.parse(pa.optString(i)));renderItems();renderPhotos();}

    private void createPdfAndShare(){try{File dir=getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);if(dir==null)throw new Exception();if(!dir.exists())dir.mkdirs();String safe=text(clientName).replaceAll("[^\\p{L}\\p{N}_-]","_");if(safe.isEmpty())safe="client";File f=new File(dir,"Organizer_Form_"+safe+"_"+System.currentTimeMillis()+".pdf");PdfDocument doc=new PdfDocument();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());Canvas c=pg.getCanvas();c.drawColor(Color.WHITE);p.setTextAlign(Paint.Align.RIGHT);p.setColor(DARK_GREEN);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(22);c.drawText("استمارة طلب عرض سعر منظومة شمسية",560,42,p);p.setTextSize(12);p.setColor(GREEN);c.drawText("INFINITY GREEN POWER",560,62,p);p.setColor(GREEN);c.drawRect(35,78,560,81,p);int y=100;y=pdfSection(c,p,y,"معلومات العميل");y=pdfLine(c,p,y,"اسم العميل",text(clientName));y=pdfLine(c,p,y,"رقم الهاتف",text(phone));y=pdfLine(c,p,y,"الموقع",text(location));y=pdfLine(c,p,y,"التاريخ",text(date));y=pdfSection(c,p,y+6,"معلومات المنظومة");y=pdfLine(c,p,y,"نوع المنظومة",spin(systemType));y=pdfLine(c,p,y,"السعة المطلوبة",text(capacity)+(text(capacity).isEmpty()?"":" kW"));y=pdfLine(c,p,y,"الطور",spin(phase));y=pdfSection(c,p,y+6,"المواد والأسعار");y=pdfItems(c,p,y);y=pdfSection(c,p,y+8,"معلومات إضافية");y=pdfLine(c,p,y,"التركيب",installation.isChecked()?"نعم":"لا");y=pdfLine(c,p,y,"النقل",transport.isChecked()?"نعم":"لا");y=pdfLine(c,p,y,"اسم منظم الكشف",text(organizer));y=pdfLine(c,p,y,"ملاحظات",trim(text(notes),55));p.setColor(MUTED);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(9);c.drawText("Organizer Form • Infinity Green Power",297,822,p);doc.finishPage(pg);
        for(int i=0;i<photos.size();i++){Bitmap b=decode(photos.get(i));if(b==null)continue;PdfDocument.Page pp=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,i+2).create());Canvas cc=pp.getCanvas();cc.drawColor(Color.WHITE);p.setTextAlign(Paint.Align.RIGHT);p.setColor(DARK_GREEN);p.setTextSize(19);p.setTypeface(Typeface.create("sans",Typeface.BOLD));cc.drawText("صورة الموقع / المنظومة",560,45,p);float sc=Math.min(500f/b.getWidth(),690f/b.getHeight());int w=(int)(b.getWidth()*sc),h=(int)(b.getHeight()*sc),left=(595-w)/2,top=95;cc.drawBitmap(b,null,new Rect(left,top,left+w,top+h),null);doc.finishPage(pp);b.recycle();}
        FileOutputStream out=new FileOutputStream(f);doc.writeTo(out);out.close();doc.close();Uri uri=new Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(f.getName()).build();Intent share=new Intent(Intent.ACTION_SEND);share.setType("application/pdf");share.putExtra(Intent.EXTRA_STREAM,uri);share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(share,"مشاركة الاستمارة"));}catch(Exception e){Toast.makeText(this,"تعذر إنشاء PDF",Toast.LENGTH_LONG).show();}}
    private int pdfSection(Canvas c,Paint p,int y,String s){p.setColor(NAVY);c.drawRoundRect(35,y,560,y+24,8,8,p);p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.RIGHT);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(12);c.drawText(s,548,y+17,p);return y+34;}
    private int pdfLine(Canvas c,Paint p,int y,String l,String v){p.setTextAlign(Paint.Align.RIGHT);p.setColor(TEXT);p.setTextSize(10);p.setTypeface(Typeface.create("sans",Typeface.BOLD));c.drawText(l+" :",555,y+15,p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));c.drawText(v.isEmpty()?"—":v,420,y+15,p);p.setColor(Color.rgb(230,236,232));c.drawLine(35,y+23,560,y+23,p);return y+27;}
    private int pdfItems(Canvas c,Paint p,int y){int[]x={560,405,270,180,95,35};p.setColor(GREEN);c.drawRect(35,y,560,y+26,p);p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(9);p.setTypeface(Typeface.create("sans",Typeface.BOLD));String[]h={"المادة","الموديل","السعة","الكمية","السعر"};for(int i=0;i<5;i++)c.drawText(h[i],(x[i]+x[i+1])/2f,y+17,p);y+=26;if(items.isEmpty()){p.setColor(MUTED);c.drawText("لا توجد مواد",297,y+18,p);return y+30;}for(Item it:items){p.setColor(Color.rgb(249,252,250));c.drawRect(35,y,560,y+28,p);p.setColor(BORDER);for(int z:x)c.drawLine(z,y,z,y+28,p);c.drawLine(35,y+28,560,y+28,p);p.setColor(TEXT);p.setTextSize(8);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));c.drawText(trim(it.name,14),(x[0]+x[1])/2f,y+17,p);c.drawText(trim(it.model,14),(x[1]+x[2])/2f,y+17,p);c.drawText(trim(it.spec,11),(x[2]+x[3])/2f,y+17,p);c.drawText(trim(it.qty,7),(x[3]+x[4])/2f,y+17,p);c.drawText(trim(it.price,8),(x[4]+x[5])/2f,y+17,p);y+=28;}return y;}

    private Bitmap decode(Uri u){try{InputStream in=getContentResolver().openInputStream(u);BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeStream(in,null,b);if(in!=null)in.close();int s=1;while(b.outWidth/s>1200||b.outHeight/s>1200)s*=2;BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=s;InputStream in2=getContentResolver().openInputStream(u);Bitmap bm=BitmapFactory.decodeStream(in2,null,o);if(in2!=null)in2.close();return bm;}catch(Exception e){return null;}}
    private String displayName(Uri u){try{Cursor c=getContentResolver().query(u,null,null,null,null);if(c!=null){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(c.moveToFirst()&&i>=0){String n=c.getString(i);c.close();return n;}c.close();}}catch(Exception ignored){}return "صورة";}

    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(15),dp(15),dp(15));c.setBackground(round(Color.WHITE,22,BORDER,1));c.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));if(Build.VERSION.SDK_INT>=21)c.setElevation(dp(2));return c;}
    private View empty(String t,String s){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(14),dp(14),dp(14),dp(14));b.setBackground(round(Color.rgb(250,252,251),16,BORDER,1));b.addView(txt(t,14,true,TEXT));TextView x=txt(s,12,false,MUTED);x.setPadding(0,dp(4),0,0);b.addView(x);return b;}
    private Button primary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT_BOLD);b.setBackground(round(GREEN,17,GREEN,0));b.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(52)));return b;}
    private Button outlineBtn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(GREEN);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT_BOLD);b.setBackground(round(Color.WHITE,16,BORDER,1));b.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(46)));return b;}
    private Button smallBtn(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setTextColor(fg);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT_BOLD);b.setBackground(round(bg,14,bg,0));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(42),1);lp.leftMargin=dp(4);lp.rightMargin=dp(4);b.setLayoutParams(lp);return b;}
    private TextView tag(String s,int fg,int bg){TextView t=txt(s,11,true,fg);t.setBackground(round(bg,12,bg,0));t.setPadding(dp(9),dp(5),dp(9),dp(5));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(6);t.setLayoutParams(lp);return t;}
    private TextView txt(String s,int sz,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sz);t.setTextColor(color);t.setTextDirection(View.TEXT_DIRECTION_RTL);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);return t;}
    private GradientDrawable round(int fill,int r,int stroke,int sw){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(r));if(sw>0)d.setStroke(dp(sw),stroke);return d;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}private String text(EditText e){return e==null?"":e.getText().toString().trim();}private String safe(String s){return s==null||s.trim().isEmpty()?"—":s;}private String spin(Spinner s){if(s==null||s.getSelectedItem()==null)return"";String v=String.valueOf(s.getSelectedItem());return v.equals("اختر")?"":v;}private void select(Spinner s,String v){for(int i=0;i<s.getCount();i++)if(String.valueOf(s.getItemAtPosition(i)).equals(v)){s.setSelection(i);break;}}private String trim(String s,int n){if(s==null)return"";return s.length()<=n?s:s.substring(0,n-1)+"…";}
}
