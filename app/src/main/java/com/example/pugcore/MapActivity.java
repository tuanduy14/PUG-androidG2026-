package com.example.pugcore;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Bản đồ 10 ván (dọc): chọn ván rồi vào BattleActivity (ngang). Thay cho hộp thoại "Chọn quái". */
public class MapActivity extends AppCompatActivity {

    private PlayerData player;
    private StageMapView map;
    private ScrollView scroll;
    private int selected = -1;
    private boolean firstShow = true;

    private View footer, bossTag;
    private PixelSpriteView icon;
    private TextView tvCount, tvName, tvStats, btnFight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PixelUi.init(this);
        SoundFx.init(this);
        setContentView(R.layout.activity_map);
        UiUtil.applySystemBarPadding(findViewById(R.id.root));
        SettingsDialog.attach(this);

        map = findViewById(R.id.stageMap);
        scroll = findViewById(R.id.mapScroll);
        footer = findViewById(R.id.mapFooter);
        bossTag = findViewById(R.id.tvMapBossTag);
        icon = findViewById(R.id.mapMonsterIcon);
        tvCount = findViewById(R.id.tvMapCount);
        tvName = findViewById(R.id.tvMapStageName);
        tvStats = findViewById(R.id.tvMapStageStats);
        btnFight = findViewById(R.id.btnFight);

        findViewById(R.id.mapHeader).setBackground(PixelUi.box(this, 0xFF161C2C, PixelUi.OUTLINE, 3));
        findViewById(R.id.mapMonsterBox).setBackground(PixelUi.box(this, 0xFF0D111C, PixelUi.OUTLINE, 2));
        bossTag.setBackgroundColor(0xFFE11D48);
        TextView back = findViewById(R.id.btnBack);
        back.setBackground(PixelUi.button(this, 0xFF28314B));
        back.setOnClickListener(v -> finish());

        map.setListener(stage -> { selected = stage; SoundFx.play(SoundFx.TAB); refresh(); });
        btnFight.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            final int idx = selected - 1; // MONSTERS[i] là ván i+1
            UiUtil.askRotateThen(this, () -> {
                Intent i = new Intent(this, BattleActivity.class);
                i.putExtra(BattleActivity.EXTRA_MONSTER, idx);
                startActivity(i);
            });
        });
        PixelUi.applyFont(findViewById(android.R.id.content));
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicPlayer.play(this, MusicPlayer.MENU);
        player = PlayerData.load(this); // vừa thắng xong thì đã mở khoá ván mới
        if (selected < 1 || selected > player.maxStage) selected = player.maxStage;
        refresh();
        scrollToSelected();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicPlayer.pause();
    }

    private void refresh() {
        map.setState(player.maxStage, player.clearedArray(), selected);
        GameData.Stage s = GameData.stage(selected);
        tvCount.setText(player.clearedCount() + "/" + GameData.TOTAL_STAGES);
        icon.setMonster(s.archetype);
        bossTag.setVisibility(s.isBoss ? View.VISIBLE : View.GONE);
        tvName.setText("Ván " + s.stage + ": " + s.name);
        tvStats.setText("HP " + s.maxHp + " · ATK " + s.atk + (player.isCleared(s.stage)
                ? " · ĐÃ THẮNG (không thưởng)" : " · +" + s.goldReward + " vàng"));
        footer.setBackgroundColor(s.isBoss ? 0xFF2C1520 : 0xFF161C2C);
        btnFight.setBackground(PixelUi.button(this, s.isBoss ? 0xFFF43F5E : 0xFFF59E0B));
        btnFight.setTextColor(s.isBoss ? 0xFFFFFFFF : PixelUi.DARK_TEXT);
        btnFight.setTypeface(PixelUi.font);
    }

    private void scrollToSelected() {
        if (map.getHeight() > 0) {
            scroll.post(() -> scroll.scrollTo(0, Math.max(0, (int) map.nodeCenterY(selected) - scroll.getHeight() / 2)));
            return;
        }
        // lần đầu: chờ view đo xong chiều cao
        map.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                if (map.getHeight() == 0) return;
                map.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                scroll.scrollTo(0, Math.max(0, (int) map.nodeCenterY(selected) - scroll.getHeight() / 2));
            }
        });
    }
}
