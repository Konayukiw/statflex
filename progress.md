# statflex GUI 移植 Progress

WLR (`Projects/WLR/.../gui`) のGUI実装をJavaに書き直し、statflex に移植する作業ログ。

## ToDo

- [x] 1. progress.md 作成・作業方針の確定
- [x] 2. `gui` パッケージ作成 / `GuiColors.java` 作成
- [x] 3. `elements/GuiComponentBase.java` 作成（定数含む）
- [x] 4. `elements/Button.java` 作成
- [x] 5. `elements/Checkbox.java` 作成
- [x] 6. `elements/Slider.java` 作成
- [x] 7. `elements/Textfield.java` 作成
- [x] 8. `elements/Dropdown.java` 作成
- [x] 9. `ConfigGui.java` 作成（Settings / Toggles 全項目を配置）
- [x] 10. `Commands.java` を `/s` 単独で GUI を開くよう変更
- [x] 11. コンパイル確認・最終チェック

## 状況メモ

- **完了**: `.\gradlew.bat compileJava` → **BUILD SUCCESSFUL**
- **パッケージ**: `com.konayuki.statflex.gui` / `com.konayuki.statflex.gui.elements`
- **起動**: `/s`（引数なし）で `ConfigGui` を表示
- **タブ構成**:
  | タブ | 操作できる項目 |
  |------|----------------|
  | Toggles | Denick / Stats List / Auto Duels / Updated Duels Titles / Secure Connection / Keep /who |
  | General | API Key / Skin Save Dir / Flag Interval |
  | Warnings | warnLevel / warnFKDR |
  | AutoGG | autoGGMessages（`|` 区切り）+ Save ボタン |
- **既存コード**: Settings / Toggles 本体は未変更。Commands の no-args 分岐のみ変更。
