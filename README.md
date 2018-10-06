# SimpleBLE


## 內容

- APP 一開啟時開始偵測附近的 BLE 藍牙裝置(TextLabel 顯示尋找藍牙裝置)，
- 當找到裝置的 UUID 符合 6E400001-B5A3-F393-E0A9-E50E24DCCA9E 進行連線(TextLabel 顯示已連接藍牙裝置)，
- 當斷線時又恢復偵測附近的 BLE 藍牙裝置的狀態 (TextLabel 顯示尋找藍牙裝置)
- 連線後註冊 BLE 藍牙裝置 
	+ Write Service : 6E400002-B5A3-F393-E0A9-E50E24DCCA9E 
	+ Notify Service : 6E400003-B5A3-F393-E0A9-E50E24DCCA9E