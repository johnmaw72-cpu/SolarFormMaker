package com.infinitygreenpower.solarform;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int NAVY=Color.rgb(11,49,93), GREEN=Color.rgb(46,139,46), BG=Color.rgb(245,247,250), BORDER=Color.rgb(220,225,232);
    private final int PICK_IMAGES=2201;
    private LinearLayout root, selectedItemsBox, photosBox;
    private EditText clientName, phone, location, date, sizeKw, notes, organizer;
    private RadioGroup systemType, phase;
    private CheckBox installation, transport;
    private final ArrayList<Item> selectedItems=new ArrayList<>();
    private final ArrayList<Uri> photoUris=new ArrayList<>();
    private final ArrayList<Product> catalog=new ArrayList<>();

    static class Product {
        String material, model, capacity;
        Product(String material,String model,String capacity){this.material=material;this.model=model;this.capacity=capacity;}
        String display(){return material+" — "+model+(capacity.isEmpty()?"":" — "+capacity);}
        String searchable(){return (material+" "+model+" "+capacity).toLowerCase(Locale.ROOT);}
    }
    static class Item {
        String material, model, capacity, qty, price;
        Item(String material,String model,String capacity,String qty,String price){this.material=material;this.model=model;this.capacity=capacity;this.qty=qty;this.price=price;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);
        seedCatalog();
        buildUi();
        loadDraft();
    }

    private void seedCatalog(){
        // Solar panels
        catalog.add(new Product("الألواح الشمسية","Huasun HJT","610 W"));
        catalog.add(new Product("الألواح الشمسية","Huasun HJT","615 W"));
        catalog.add(new Product("الألواح الشمسية","Huasun HJT","620 W"));
        // Hybrid inverters
        catalog.add(new Product("الإنفرتر","Roypow RS6500 Hybrid","6.5 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid - Single Phase","6 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid - Single Phase","8 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid - Single Phase","12 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid - Single Phase","16 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid - Three Phase","12 kW"));
        catalog.add(new Product("الإنفرتر","Deye Hybrid - Three Phase","16 kW"));
        catalog.add(new Product("الإنفرتر","Deye On-Grid Inverter","حدد السعة"));
        // Batteries / BMS
        catalog.add(new Product("البطارية","Roypow LiFePO4","16 kWh"));
        catalog.add(new Product("البطارية","Deye High Voltage Battery Rack","HV"));
        catalog.add(new Product("BMS","Deye High Voltage BMS","HV"));
        // Common accessories
        catalog.add(new Product("القواعد","هيكل تثبيت ألواح شمسية","حسب الموقع"));
        catalog.add(new Product("كيبل DC","Solar DC Cable","حدد القياس"));
        catalog.add(new Product("كيبل AC","AC Cable","حدد القياس"));
        catalog.add(new Product("كيبل البطاريات","Battery Cable","حدد القياس"));
        catalog.add(new Product("صندوق حماية / تجميع","Protection Box / Combiner",""));
        catalog.add(new Product("قاطع DC","DC Breaker",""));
        catalog.add(new Product("قاطع AC","AC Breaker",""));
        catalog.add(new Product("حماية DC","DC SPD",""));
        catalog.add(new Product("حماية AC","AC SPD",""));
        catalog.add(new Product("موصلات","MC4 Connectors",""));
        catalog.add(new Product("لوحة توزيع","Distribution Board (DB)",""));
        catalog.add(new Product("تأريض","Earthing System",""));
        catalog.add(new Product("قضيب أرضي","Earth Rod",""));
        catalog.add(new Product("مسار كيبل","Cable Tray / PVC",""));
        catalog.add(new Product("تركيب","أجور تركيب",""));
    }

    private TextView tv(String text,int size,boolean bold){
        TextView v=new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(Color.rgb(25,35,45));
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); v.setTextDirection(View.TEXT_DIRECTION_RTL); v.setPadding(dp(6),dp(5),dp(6),dp(5));
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return v;
    }
    private EditText field(String hint){
        EditText e=new EditText(this); e.setHint(hint); e.setTextSize(15); e.setSingleLine(true); e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); e.setTextDirection(View.TEXT_DIRECTION_RTL);
        e.setBackgroundColor(Color.WHITE); e.setPadding(dp(12),0,dp(12),0); e.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(44))); return e;
    }
    private void section(String title){
        TextView h=tv(title,17,true); h.setTextColor(Color.WHITE); h.setBackgroundColor(NAVY); h.setPadding(dp(14),dp(9),dp(14),dp(9));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(11),0,dp(6)); h.setLayoutParams(lp); root.addView(h);
    }
    private void addLabelField(String label, EditText e){ root.addView(tv(label,14,true)); root.addView(e); }
    private RadioButton rb(String text){ RadioButton b=new RadioButton(this); b.setText(text); b.setTextSize(14); b.setTextDirection(View.TEXT_DIRECTION_RTL); b.setPadding(dp(2),0,dp(4),0); return b; }
    private Button button(String text,int color){
        Button b=new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setBackgroundColor(color);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1); lp.setMargins(dp(3),0,dp(3),0); b.setLayoutParams(lp); return b;
    }

    private void buildUi(){
        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(BG); scroll.setFillViewport(true);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(28),dp(14),dp(28)); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); scroll.addView(root);

        TextView title=tv("استمارة طلب عرض سعر منظومة شمسية",21,true); title.setTextColor(NAVY); title.setGravity(Gravity.CENTER); title.setPadding(dp(8),dp(6),dp(8),dp(2)); root.addView(title);
        TextView sub=tv("Infinity Green Power  •  نموذج مبسط",13,false); sub.setTextColor(GREEN); sub.setGravity(Gravity.CENTER); sub.setPadding(dp(4),0,dp(4),dp(7)); root.addView(sub);

        section("1  معلومات العميل");
        clientName=field("اسم العميل"); phone=field("رقم الهاتف"); location=field("الموقع"); date=field("التاريخ");
        date.setText(new SimpleDateFormat("yyyy/MM/dd",Locale.US).format(new Date()));
        addLabelField("اسم العميل",clientName); addLabelField("رقم الهاتف",phone); addLabelField("الموقع",location); addLabelField("التاريخ",date);

        section("2  معلومات المنظومة");
        root.addView(tv("نوع المنظومة",14,true));
        systemType=new RadioGroup(this); systemType.setOrientation(RadioGroup.HORIZONTAL); systemType.setGravity(Gravity.RIGHT); systemType.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        for(String x:new String[]{"هجين","أون كريد","أوف كريد"}) systemType.addView(rb(x)); root.addView(systemType);
        sizeKw=field("مثال: 8"); addLabelField("السعة المطلوبة (kW)",sizeKw);
        root.addView(tv("الطور",14,true));
        phase=new RadioGroup(this); phase.setOrientation(RadioGroup.HORIZONTAL); phase.setGravity(Gravity.RIGHT); phase.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        phase.addView(rb("أحادي")); phase.addView(rb("ثلاثي")); root.addView(phase);

        section("3  المواد المطلوبة");
        TextView help=tv("ابحث عن المنتج واختره، أو أضف مادة مخصصة.",13,false); help.setTextColor(Color.DKGRAY); root.addView(help);
        LinearLayout addButtons=new LinearLayout(this); addButtons.setOrientation(LinearLayout.HORIZONTAL); addButtons.setGravity(Gravity.CENTER);
        Button searchBtn=button("بحث واختيار مادة",NAVY); Button customBtn=button("+ مادة مخصصة",GREEN); addButtons.addView(searchBtn); addButtons.addView(customBtn); root.addView(addButtons);
        selectedItemsBox=new LinearLayout(this); selectedItemsBox.setOrientation(LinearLayout.VERTICAL); selectedItemsBox.setPadding(0,dp(7),0,0); root.addView(selectedItemsBox);
        searchBtn.setOnClickListener(v->showCatalogDialog()); customBtn.setOnClickListener(v->showCustomItemDialog());

        section("4  صور الموقع / المنظومة");
        TextView photoHelp=tv("يمكن إرفاق عدة صور لمكان الألواح، الإنفرتر، البطاريات أو اللوحة الرئيسية.",13,false); photoHelp.setTextColor(Color.DKGRAY); root.addView(photoHelp);
        Button photoBtn=new Button(this); photoBtn.setText("+ إرفاق صور من الهاتف"); photoBtn.setTextColor(Color.WHITE); photoBtn.setBackgroundColor(NAVY); root.addView(photoBtn,new LinearLayout.LayoutParams(-1,dp(46)));
        HorizontalScrollView hsv=new HorizontalScrollView(this); hsv.setFillViewport(false); photosBox=new LinearLayout(this); photosBox.setOrientation(LinearLayout.HORIZONTAL); photosBox.setPadding(0,dp(8),0,dp(2)); hsv.addView(photosBox); root.addView(hsv,new LinearLayout.LayoutParams(-1,dp(120)));
        photoBtn.setOnClickListener(v->pickImages());

        section("5  معلومات إضافية");
        LinearLayout checks=new LinearLayout(this); checks.setOrientation(LinearLayout.HORIZONTAL); checks.setGravity(Gravity.RIGHT); checks.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        installation=new CheckBox(this); installation.setText("التركيب"); transport=new CheckBox(this); transport.setText("النقل"); checks.addView(installation); checks.addView(transport); root.addView(checks);
        notes=field("ملاحظات إضافية"); notes.setSingleLine(false); notes.setGravity(Gravity.TOP|Gravity.RIGHT); notes.setPadding(dp(12),dp(9),dp(12),dp(9)); notes.setMinLines(2); notes.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(78))); addLabelField("ملاحظات",notes);
        organizer=field("اسم منظم الكشف"); addLabelField("اسم منظم الكشف",organizer);

        LinearLayout buttons=new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL); buttons.setGravity(Gravity.CENTER); buttons.setPadding(0,dp(16),0,0);
        Button save=button("حفظ",NAVY); Button pdf=button("إنشاء PDF",GREEN); Button clear=button("جديد",Color.DKGRAY); buttons.addView(save); buttons.addView(pdf); buttons.addView(clear); root.addView(buttons);
        save.setOnClickListener(v->{syncItemsFromCards();saveDraft();Toast.makeText(this,"تم حفظ المسودة",Toast.LENGTH_SHORT).show();});
        pdf.setOnClickListener(v->{syncItemsFromCards();saveDraft();createPdfAndShare();});
        clear.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("مسح جميع البيانات وبدء نموذج جديد؟").setPositiveButton("نعم",(d,w)->clearForm()).setNegativeButton("لا",null).show());
        setContentView(scroll);
    }

    private void showCatalogDialog(){
        final Dialog dialog=new Dialog(this); dialog.setTitle("اختيار مادة");
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(14),dp(14),dp(14),dp(14)); box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText search=field("بحث: Huasun, Deye, Roypow, كيبل..."); box.addView(search);
        ListView list=new ListView(this); box.addView(list,new LinearLayout.LayoutParams(-1,dp(420)));
        final ArrayList<Product> filtered=new ArrayList<>(catalog); final ArrayList<String> names=new ArrayList<>(); for(Product p:filtered)names.add(p.display());
        final ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,names); list.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){
                String q=s.toString().trim().toLowerCase(Locale.ROOT); filtered.clear(); names.clear();
                for(Product p:catalog) if(q.isEmpty()||p.searchable().contains(q)){filtered.add(p);names.add(p.display());}
                adapter.notifyDataSetChanged();
            } public void afterTextChanged(Editable e){}
        });
        list.setOnItemClickListener((p,v,pos,id)->{ Product x=filtered.get(pos); addSelectedItem(new Item(x.material,x.model,x.capacity,"1","")); dialog.dismiss(); });
        dialog.setContentView(box); Window w=dialog.getWindow(); if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.94),WindowManager.LayoutParams.WRAP_CONTENT); dialog.show(); if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.94),WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void showCustomItemDialog(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(8),dp(16),0); box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText mat=field("اسم المادة"); EditText model=field("الماركة / الموديل"); EditText cap=field("السعة / القياس"); EditText qty=field("الكمية"); EditText price=field("السعر إن وجد"); qty.setText("1");
        box.addView(mat);box.addView(model);box.addView(cap);box.addView(qty);box.addView(price);
        new AlertDialog.Builder(this).setTitle("إضافة مادة مخصصة").setView(box).setPositiveButton("إضافة",(d,w)->{
            String m=t(mat); if(m.isEmpty())m="مادة أخرى"; addSelectedItem(new Item(m,t(model),t(cap),t(qty),t(price)));
        }).setNegativeButton("إلغاء",null).show();
    }

    private void addSelectedItem(Item item){ selectedItems.add(item); renderSelectedItems(); }

    private void renderSelectedItems(){
        selectedItemsBox.removeAllViews();
        if(selectedItems.isEmpty()){ TextView empty=tv("لم تتم إضافة مواد بعد.",13,false); empty.setTextColor(Color.GRAY); selectedItemsBox.addView(empty); return; }
        for(int index=0;index<selectedItems.size();index++){
            final int idx=index; Item item=selectedItems.get(index);
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(9),dp(7),dp(9),dp(8)); card.setBackgroundColor(Color.WHITE); card.setTag(idx);
            LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,-2); clp.setMargins(0,dp(4),0,dp(4)); card.setLayoutParams(clp);
            LinearLayout head=new LinearLayout(this); head.setOrientation(LinearLayout.HORIZONTAL); head.setGravity(Gravity.CENTER_VERTICAL); head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            TextView name=tv(item.material,15,true); name.setTextColor(NAVY); Button del=new Button(this); del.setText("حذف"); del.setTextSize(12); del.setTextColor(Color.rgb(160,20,20)); del.setBackgroundColor(Color.TRANSPARENT);
            head.addView(name,new LinearLayout.LayoutParams(0,dp(38),1)); head.addView(del,new LinearLayout.LayoutParams(dp(70),dp(38))); card.addView(head);
            EditText model=smallField("الماركة / الموديل",item.model); card.addView(model);
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            EditText cap=smallField("السعة / القياس",item.capacity); EditText qty=smallField("الكمية",item.qty); EditText price=smallField("السعر",item.price);
            row.addView(cap,new LinearLayout.LayoutParams(0,dp(42),2)); row.addView(qty,new LinearLayout.LayoutParams(0,dp(42),1)); row.addView(price,new LinearLayout.LayoutParams(0,dp(42),1)); card.addView(row);
            card.setTag(new EditText[]{model,cap,qty,price});
            del.setOnClickListener(v->{syncItemsFromCards(); if(idx<selectedItems.size())selectedItems.remove(idx); renderSelectedItems();});
            selectedItemsBox.addView(card);
        }
    }
    private EditText smallField(String hint,String value){ EditText e=field(hint); e.setText(value); e.setTextSize(13); e.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(42))); return e; }

    private void syncItemsFromCards(){
        int count=Math.min(selectedItems.size(),selectedItemsBox.getChildCount());
        for(int i=0;i<count;i++){
            View v=selectedItemsBox.getChildAt(i); Object tag=v.getTag(); if(!(tag instanceof EditText[]))continue;
            EditText[] f=(EditText[])tag; Item it=selectedItems.get(i); it.model=t(f[0]);it.capacity=t(f[1]);it.qty=t(f[2]);it.price=t(f[3]);
        }
    }

    private void pickImages(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_IMAGES);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data); if(requestCode!=PICK_IMAGES||resultCode!=RESULT_OK||data==null)return;
        int flags=data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if(data.getClipData()!=null){
            for(int n=0;n<data.getClipData().getItemCount();n++) addPhotoUri(data.getClipData().getItemAt(n).getUri(),flags);
        } else if(data.getData()!=null) addPhotoUri(data.getData(),flags);
        renderPhotos(); saveDraft();
    }
    private void addPhotoUri(Uri uri,int flags){
        if(uri==null)return; for(Uri u:photoUris)if(u.toString().equals(uri.toString()))return;
        try{getContentResolver().takePersistableUriPermission(uri,flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        photoUris.add(uri);
    }
    private void renderPhotos(){
        photosBox.removeAllViews();
        if(photoUris.isEmpty()){ TextView t=tv("لا توجد صور مرفقة",13,false);t.setTextColor(Color.GRAY);photosBox.addView(t);return; }
        for(int i=0;i<photoUris.size();i++){
            final int idx=i; LinearLayout cell=new LinearLayout(this); cell.setOrientation(LinearLayout.VERTICAL); cell.setGravity(Gravity.CENTER); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(102),dp(108));cp.setMargins(dp(3),0,dp(3),0);cell.setLayoutParams(cp);
            ImageView im=new ImageView(this); im.setScaleType(ImageView.ScaleType.CENTER_CROP); try{im.setImageURI(photoUris.get(i));}catch(Exception ignored){} cell.addView(im,new LinearLayout.LayoutParams(dp(96),dp(78)));
            TextView rm=tv("حذف",12,true);rm.setTextColor(Color.rgb(170,30,30));rm.setGravity(Gravity.CENTER);rm.setOnClickListener(v->{photoUris.remove(idx);renderPhotos();saveDraft();});cell.addView(rm,new LinearLayout.LayoutParams(dp(96),dp(28)));photosBox.addView(cell);
        }
    }

    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private String t(EditText e){return e.getText().toString().trim();}
    private String checkedText(RadioGroup g){int id=g.getCheckedRadioButtonId(); if(id==-1)return ""; return ((RadioButton)findViewById(id)).getText().toString();}

    private void saveDraft(){
        syncItemsFromCards(); SharedPreferences.Editor e=getSharedPreferences("draft",0).edit();
        e.putString("client",t(clientName)).putString("phone",t(phone)).putString("location",t(location)).putString("date",t(date)).putString("kw",t(sizeKw)).putString("notes",t(notes)).putString("organizer",t(organizer));
        e.putInt("system",systemType.getCheckedRadioButtonId()).putInt("phase",phase.getCheckedRadioButtonId()).putBoolean("install",installation.isChecked()).putBoolean("transport",transport.isChecked());
        try{
            JSONArray a=new JSONArray(); for(Item it:selectedItems){JSONObject o=new JSONObject();o.put("material",it.material);o.put("model",it.model);o.put("capacity",it.capacity);o.put("qty",it.qty);o.put("price",it.price);a.put(o);} e.putString("items",a.toString());
            JSONArray p=new JSONArray(); for(Uri u:photoUris)p.put(u.toString()); e.putString("photos",p.toString());
        }catch(Exception ignored){}
        e.apply();
    }
    private void loadDraft(){
        SharedPreferences p=getSharedPreferences("draft",0); clientName.setText(p.getString("client","")); phone.setText(p.getString("phone","")); location.setText(p.getString("location",""));
        String d=p.getString("date",""); if(!d.isEmpty())date.setText(d); sizeKw.setText(p.getString("kw","")); notes.setText(p.getString("notes","")); organizer.setText(p.getString("organizer",""));
        int s=p.getInt("system",-1),ph=p.getInt("phase",-1); if(s!=-1)systemType.check(s); if(ph!=-1)phase.check(ph); installation.setChecked(p.getBoolean("install",false)); transport.setChecked(p.getBoolean("transport",false));
        selectedItems.clear(); photoUris.clear();
        try{JSONArray a=new JSONArray(p.getString("items","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);selectedItems.add(new Item(o.optString("material"),o.optString("model"),o.optString("capacity"),o.optString("qty"),o.optString("price")));}}catch(Exception ignored){}
        try{JSONArray a=new JSONArray(p.getString("photos","[]"));for(int i=0;i<a.length();i++)photoUris.add(Uri.parse(a.getString(i)));}catch(Exception ignored){}
        renderSelectedItems(); renderPhotos();
    }
    private void clearForm(){
        getSharedPreferences("draft",0).edit().clear().apply(); clientName.setText("");phone.setText("");location.setText("");sizeKw.setText("");notes.setText("");organizer.setText("");systemType.clearCheck();phase.clearCheck();installation.setChecked(false);transport.setChecked(false);selectedItems.clear();photoUris.clear();renderSelectedItems();renderPhotos();date.setText(new SimpleDateFormat("yyyy/MM/dd",Locale.US).format(new Date()));
    }

    private void createPdfAndShare(){
        try{
            File dir=getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS); if(dir==null)throw new IOException("Storage unavailable"); if(!dir.exists())dir.mkdirs();
            String safe=t(clientName).replaceAll("[^\\p{L}\\p{N}_-]","_"); if(safe.isEmpty())safe="client"; File file=new File(dir,"Solar_Form_"+safe+"_"+System.currentTimeMillis()+".pdf");
            PdfDocument doc=new PdfDocument(); Paint p=new Paint(1); int pageNo=1;
            PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo++).create()); Canvas c=page.getCanvas(); c.drawColor(Color.WHITE); drawHeader(c,p); int y=105; y=drawClient(c,p,y); y=drawSystem(c,p,y+5); y=drawItems(c,p,y+5,0,Math.min(selectedItems.size(),9)); y=drawFooter(c,p,y+5); doc.finishPage(page);
            if(selectedItems.size()>9){
                int start=9; while(start<selectedItems.size()){page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo++).create());c=page.getCanvas();c.drawColor(Color.WHITE);drawHeader(c,p);int end=Math.min(start+18,selectedItems.size());drawItems(c,p,110,start,end);doc.finishPage(page);start=end;}
            }
            for(Uri uri:photoUris){
                Bitmap bm=loadBitmap(uri); if(bm==null)continue; page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo++).create()); c=page.getCanvas();c.drawColor(Color.WHITE);drawHeader(c,p);rtl(p,15,NAVY,Paint.Align.RIGHT,true);c.drawText("صورة مرفقة من موقع المنظومة",565,110,p);Rect dst=fitRect(bm.getWidth(),bm.getHeight(),35,135,525,650);c.drawBitmap(bm,null,dst,p);doc.finishPage(page);bm.recycle();
            }
            FileOutputStream out=new FileOutputStream(file);doc.writeTo(out);out.close();doc.close();
            Uri uri=new Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(file.getName()).build(); Intent share=new Intent(Intent.ACTION_SEND);share.setType("application/pdf");share.putExtra(Intent.EXTRA_STREAM,uri);share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(share,"مشاركة النموذج"));
        }catch(Exception ex){Toast.makeText(this,"تعذر إنشاء PDF: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private Bitmap loadBitmap(Uri uri){try(InputStream in=getContentResolver().openInputStream(uri)){return BitmapFactory.decodeStream(in);}catch(Exception e){return null;}}
    private Rect fitRect(int bw,int bh,int x,int y,int maxW,int maxH){float s=Math.min((float)maxW/bw,(float)maxH/bh);int w=(int)(bw*s),h=(int)(bh*s);int dx=x+(maxW-w)/2,dy=y+(maxH-h)/2;return new Rect(dx,dy,dx+w,dy+h);}
    private void rtl(Paint p,float size,int color,Paint.Align align,boolean bold){p.setTextSize(size);p.setColor(color);p.setTextAlign(align);p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));}
    private void drawHeader(Canvas c,Paint p){rtl(p,22,NAVY,Paint.Align.RIGHT,true);c.drawText("استمارة طلب عرض سعر منظومة شمسية",565,45,p);rtl(p,12,GREEN,Paint.Align.RIGHT,false);c.drawText("INFINITY GREEN POWER",565,68,p);p.setColor(GREEN);p.setStrokeWidth(2);c.drawLine(30,84,565,84,p);}
    private int drawClient(Canvas c,Paint p,int y){y=bar(c,p,"1  معلومات العميل",y);y=line(c,p,"اسم العميل",t(clientName),y);y=line(c,p,"رقم الهاتف",t(phone),y);y=line(c,p,"الموقع",t(location),y);y=line(c,p,"التاريخ",t(date),y);return y;}
    private int drawSystem(Canvas c,Paint p,int y){y=bar(c,p,"2  معلومات المنظومة",y);y=line(c,p,"نوع المنظومة",checkedText(systemType),y);y=line(c,p,"السعة المطلوبة",t(sizeKw)+(t(sizeKw).isEmpty()?"":" kW"),y);y=line(c,p,"الطور",checkedText(phase),y);return y;}
    private int drawItems(Canvas c,Paint p,int y,int start,int end){y=bar(c,p,"3  المواد المطلوبة",y);p.setColor(GREEN);c.drawRect(30,y,565,y+27,p);rtl(p,9,Color.WHITE,Paint.Align.CENTER,true);c.drawText("المادة",500,y+18,p);c.drawText("الموديل",355,y+18,p);c.drawText("السعة",235,y+18,p);c.drawText("الكمية",145,y+18,p);c.drawText("السعر",70,y+18,p);y+=27;
        for(int i=start;i<end;i++){Item it=selectedItems.get(i);Paint bg=new Paint();bg.setColor(i%2==0?Color.rgb(248,249,251):Color.WHITE);c.drawRect(30,y,565,y+31,bg);Paint ln=new Paint();ln.setColor(BORDER);ln.setStyle(Paint.Style.STROKE);c.drawRect(30,y,565,y+31,ln);rtl(p,8.5f,Color.DKGRAY,Paint.Align.RIGHT,false);c.drawText(shorten(it.material,18),555,y+20,p);rtl(p,8.5f,Color.DKGRAY,Paint.Align.CENTER,false);c.drawText(shorten(it.model,23),355,y+20,p);c.drawText(shorten(it.capacity,14),235,y+20,p);c.drawText(shorten(it.qty,8),145,y+20,p);c.drawText(shorten(it.price,10),70,y+20,p);y+=31;}return y;}
    private int drawFooter(Canvas c,Paint p,int y){y=bar(c,p,"4  معلومات إضافية",y);y=line(c,p,"التركيب",installation.isChecked()?"نعم":"لا",y);y=line(c,p,"النقل",transport.isChecked()?"نعم":"لا",y);y=line(c,p,"اسم منظم الكشف",t(organizer),y);y=line(c,p,"عدد الصور المرفقة",String.valueOf(photoUris.size()),y);rtl(p,9.5f,Color.DKGRAY,Paint.Align.RIGHT,false);c.drawText("ملاحظات: "+shorten(t(notes),75),565,y+17,p);rtl(p,8.5f,GREEN,Paint.Align.CENTER,false);c.drawText("Solar Form Maker • Infinity Green Power",297,820,p);return y+26;}
    private int bar(Canvas c,Paint p,String s,int y){p.setColor(NAVY);c.drawRect(30,y,565,y+27,p);rtl(p,12,Color.WHITE,Paint.Align.RIGHT,true);c.drawText(s,555,y+19,p);return y+32;}
    private int line(Canvas c,Paint p,String label,String value,int y){rtl(p,10.5f,Color.DKGRAY,Paint.Align.RIGHT,true);c.drawText(label+" :",555,y+17,p);rtl(p,10.5f,Color.BLACK,Paint.Align.RIGHT,false);c.drawText(shorten(value,55),430,y+17,p);p.setColor(BORDER);c.drawLine(30,y+24,565,y+24,p);return y+27;}
    private String shorten(String s,int n){if(s==null)return "";return s.length()<=n?s:s.substring(0,Math.max(0,n-1))+"…";}
}
