
## ADS SERVER AUTOMATION

####`Tests - ORDER OF EXECUTION` 

```
NOTE : 
Targeting, CostSpendAndEventLogging & Capping have to run one after the other  
PrivateAuction & GeneralRTBSpecRegression have to run one after the other  
Dynamic Capping is independant and can run alone  
```

### 1) Targeting

`Pre test steps`
- New Campaign, LineItem, Placement & Creative are created and are Linked together.
- Status of the Campaign,LineItem,Placement & Creative updated to Running.
- Log4J properties updated to trace to capture logs from ads server.
- Bid request matching the placement and creative is sent, logs related to the placement are captured.

`Post test steps`
- Write earlier created Campaign, LineItem, Placement & Creative ID's in a property file.
- Write impressionURL, clickURL winNotifyURL & Transaction ID in a property file.

### 2) CostSpendAndEventLogging

`Pre test steps`
- Hit impressionURL-10 times, clickURL & winNotifyURL-5 times, conversion URL-5times.
- Capture data from aerospike.

`Post test steps`
- None

### 3) Capping

`Pre test steps`
- Read earlier created Campaign, LineItem & Placement & Creative ID's from a property file.
- Read impression URL from the property file.

`Post test steps`
- Delete earlier created Campaign, LineItem & Placement


### 4) PrivateAuction

`Pre test steps`
- Create new Campaign, LineItem & Placement, save them in a property file.
- New Campaign, LineItem & Placement Status updated to Running.

`Post test steps`
- Revert back placement deal ID which is changed to test a negative scenario in one of the Private Auction tests.

### 5) GeneralRTBSpecRegression

`Pre test steps`  
- Read earlier created Campaign, LineItem & Placement & Creative ID's from a property file.
- Read impression URL from the property file.

`Post test steps`
- Delete earlier created Campaign, LineItem , Creative & Placement


### 6) Dynamic Capping 

`Pre test steps`  
- Create new Campaign, LineItem ,creative & Placement, save them in a property file.
- New Campaign, LineItem & Placement Status updated to Running.

`Post test steps` 
- Delete earlier created Campaign, LineItem , Creative & Placement