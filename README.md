# SimpleBLE

一個簡單的Ble App架構範例，當App開啟時開始搜尋藍芽裝置並連線，斷線後重新搜尋。
	
## 架構介紹

- ble/BleService 負責處理藍芽連線的所有過程
- ble/BleConstants 紀載UUID和其他藍芽連線用常數。
- BaseBleActivity 負責處理 BleService 與Activity生命週期的綁定，並檢查連線藍芽所需要的 Permission，當定位、藍芽的權限接開啟時才會開始連線。
- MainActivity 繼承 BaseBleActivity，並複寫 OnConnectOn / OnConnectOff 兩個接口，只處理當藍芽狀態改變時需要實現的邏輯。
