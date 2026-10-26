package com.crmbank.erp.mobile.hpio;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.CodeDto;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.RetrofitClient;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHPIO250U] 모바일 자재불출요청 (조회 전용)
 * 웹 HPIO250U.vue 기반 100% 매칭 (순수 정보 전달/조회 전용 구현)
 * 1. 불출일자 및 생산라인별 자재 불출 요청 내역 조회 (HPIO_250U_STR)
 * 2. 출고창고/현장창고 지정 및 상세 자재 불출 요청 내역 조회
 */
public class MHPIO250U extends BaseActivity {

    private Spinner spLine, spFormLine, spWhCd, spIWhCd;
    private TextView tvStartDate, tvEndDate, tvOutYmd, tvHopeYmd;
    private EditText etBigo;
    private ListView lvDetailList;
    private DetailAdapter adapter;

    private final List<Map<String, Object>> detailList = new ArrayList<>();
    private final List<CodeDto> lineList = new ArrayList<>();
    private final List<CodeDto> whList = new ArrayList<>();

    private ApiService apiService;
    private String cmpycd = "COIT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhpio250u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "COIT").trim();

        apiService = RetrofitClient.getApiService();

        initViews();
        loadOptions();
    }

    private void initViews() {
        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        if (tvHeaderTitle != null) tvHeaderTitle.setText("자재불출요청");

        spLine = findViewById(R.id.spLine);
        spFormLine = findViewById(R.id.spFormLine);
        spWhCd = findViewById(R.id.spWhCd);
        spIWhCd = findViewById(R.id.spIWhCd);

        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvOutYmd = findViewById(R.id.tvOutYmd);
        tvHopeYmd = findViewById(R.id.tvHopeYmd);

        etBigo = findViewById(R.id.etBigo);
        if (etBigo != null) etBigo.setFocusable(false);

        lvDetailList = findViewById(R.id.lvDetailList);
        adapter = new DetailAdapter();
        lvDetailList.setAdapter(adapter);

        // 기본 날짜 세팅 (당월 1일 ~ 오늘)
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        tvEndDate.setText(today);
        tvOutYmd.setText(today);
        tvHopeYmd.setText(today);

        cal.set(Calendar.DAY_OF_MONTH, 1);
        tvStartDate.setText(sdf.format(cal.getTime()));

        tvStartDate.setOnClickListener(v -> showDatePicker(tvStartDate));
        tvEndDate.setOnClickListener(v -> showDatePicker(tvEndDate));

        findViewById(R.id.btnReset).setOnClickListener(v -> initializeForm());
        findViewById(R.id.btnSearch).setOnClickListener(v -> search());
    }

    private void loadOptions() {
        // 1. 생산라인 옵션 로드 (HP00_000S_STR L0)
        Map<String, Object> pLine = new HashMap<>();
        pLine.put("gubun", "L0");
        pLine.put("cmpycd", cmpycd);
        pLine.put("gbncd", "Y");
        pLine.put("code", "");

        apiService.executeHp00Procedure("HP00_000S_STR", pLine).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    lineList.clear();
                    for (Map<String, Object> m : response.body()) {
                        CodeDto dto = new CodeDto();
                        dto.codecd = getStringVal(m, "linecd");
                        if (dto.codecd.isEmpty()) dto.codecd = getStringVal(m, "code");
                        dto.codenm = getStringVal(m, "linenm");
                        if (dto.codenm.isEmpty()) dto.codenm = getStringVal(m, "cdnm");
                        lineList.add(dto);
                    }
                    ArrayAdapter<CodeDto> adapter = new ArrayAdapter<>(MHPIO250U.this, android.R.layout.simple_spinner_item, lineList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spLine.setAdapter(adapter);
                    spFormLine.setAdapter(adapter);

                    if (!lineList.isEmpty()) {
                        spLine.setSelection(0);
                        spFormLine.setSelection(0);
                    }
                }
            }

            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });

        // 2. 창고 옵션 로드 (HS00_000S_STR W0)
        Map<String, Object> pWh = new HashMap<>();
        pWh.put("gubun", "W0");
        pWh.put("cmpycd", cmpycd);

        apiService.executeHs00Procedure("HS00_000S_STR", pWh).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    whList.clear();
                    for (Map<String, Object> m : response.body()) {
                        CodeDto dto = new CodeDto();
                        dto.codecd = getStringVal(m, "whcd");
                        if (dto.codecd.isEmpty()) dto.codecd = getStringVal(m, "code");
                        dto.codenm = getStringVal(m, "whnm");
                        if (dto.codenm.isEmpty()) dto.codenm = getStringVal(m, "cdnm");
                        whList.add(dto);
                    }
                    ArrayAdapter<CodeDto> adapter = new ArrayAdapter<>(MHPIO250U.this, android.R.layout.simple_spinner_item, whList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spWhCd.setAdapter(adapter);
                    spIWhCd.setAdapter(adapter);

                    if (!whList.isEmpty()) {
                        spWhCd.setSelection(0);
                        if (whList.size() > 1) spIWhCd.setSelection(1);
                    }
                }
                search();
            }

            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) { search(); }
        });
    }

    private void search() {
        String selectedLine = "";
        if (spLine != null && spLine.getSelectedItem() != null) {
            selectedLine = ((CodeDto) spLine.getSelectedItem()).codecd;
        }

        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S0");
        p.put("cmpycd", cmpycd);
        p.put("linecd", selectedLine);
        p.put("fromdt", tvStartDate.getText().toString().replace("-", "").trim());
        p.put("todt", tvEndDate.getText().toString().replace("-", "").trim());

        apiService.executeHpioProcedure("HPIO_250U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detailList.clear();
                    detailList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MHPIO250U.this, "조회되었습니다. (" + detailList.size() + "건)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MHPIO250U.this, "조회된 내역이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                Toast.makeText(MHPIO250U.this, "통신 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeForm() {
        if (etBigo != null) etBigo.setText("");
        detailList.clear();
        adapter.notifyDataSetChanged();
        search();
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(tv.getText().toString());
            if (d != null) cal.setTime(d);
        } catch (Exception ignored) {}

        new DatePickerDialog(this, (view, y, m, d) -> {
            tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null || key == null) return "";
        Object val = map.get(key);
        if (val == null) val = map.get(key.toUpperCase(Locale.ROOT));
        if (val == null) val = map.get(key.toLowerCase(Locale.ROOT));
        return val != null ? String.valueOf(val).trim() : "";
    }

    private double getDoubleVal(Map<String, Object> map, String key) {
        try { return Double.parseDouble(getStringVal(map, key).replace(",", "")); } catch (Exception e) { return 0.0; }
    }

    @Override protected String getProgramTitle() { return "자재불출요청"; }
    @Override protected String getProgramId() { return "MHPIO250U"; }

    private class DetailAdapter extends BaseAdapter {
        private final DecimalFormat df = new DecimalFormat("#,###");

        @Override public int getCount() { return detailList.size(); }
        @Override public Object getItem(int p) { return detailList.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(MHPIO250U.this).inflate(R.layout.item_mhpio250u, parent, false);
                holder = new ViewHolder();
                holder.tvMItemCode = convertView.findViewById(R.id.tvMItemCode);
                holder.tvMItemName = convertView.findViewById(R.id.tvMItemName);
                holder.tvMItSize = convertView.findViewById(R.id.tvMItSize);
                holder.tvReqQty = convertView.findViewById(R.id.tvReqQty);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Map<String, Object> item = detailList.get(position);

            holder.tvMItemCode.setText(getStringVal(item, "mitemcd"));
            holder.tvMItemName.setText(getStringVal(item, "mitemnm"));
            holder.tvMItSize.setText(String.format("%s / %s", getStringVal(item, "mitsize"), getStringVal(item, "munit")));

            double reqQty = getDoubleVal(item, "reqqty");
            holder.tvReqQty.setText("요청수량: " + df.format(reqQty));

            return convertView;
        }
    }

    static class ViewHolder {
        TextView tvMItemCode, tvMItemName, tvMItSize, tvReqQty;
    }
}
