# Huzai
出来立てサーバー(猫)用の仮プラグインです。  

# 長期不在届確認プラグイン  
**コマンド説明**  
/huzai  
  add    <プレイヤー名> <期限(年)> <期限(月)> <期限(日)>  
  check  <プレイヤー名>  
  del    <プレイヤー名>  
  list
  
add    :長期不在届を登録  
check  :長期不在届確認  
del    :長期不在届を削除  
list   :長期不在届提出者一覧を表示
  
**権限説明**  
huzai.use    :長期不在届コマンドを使用する基本権限(/huzai)  
huzai.add    :長期不在届を追加する権限(/huzai add)  
huzai.check  :長期不在届を確認する権限(/huzai check)  
huzai.del    :長期不在届を削除する権限(/huzao del)  
huzai.list   :不在者リストを表示する権限(/huzai list)

**absences.yml**  
plugin/huzaiの中に*absences.yml*があります。  
こちらは、不在者のデータがあります。  
## 動画時点では、UUID非対応ですがver3はUUID対応しました。
## 動画から結構いろいろ変わってます。
