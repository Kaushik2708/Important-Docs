import sapphire.util.DataSet;
import sapphire.SapphireException;
import sapphire.xml.PropertyList;

def strSamples=inputs.u_parentlotsamples_parentsampleid.split(";");
def strIds=inputs.u_parentlotsamples_u_parentlotsamplesid.split(";");
def strChildBatchIds=inputs.u_parentlotsamples_childbatchid.split(";");
def strParentBatchIds=inputs.u_parentlotsamples_parentbatchid.split(";");

logger.info("Event Plan: SyncCOASample: SDI Created");

for(int i=0;i<strSamples.length;i++){    
// Create a Sample SDI
def AddSDIprops=[
    sdcid: "Sample",
    copies: "1"
  
]
logger.info( "Calling AddSDI with properties: " + AddSDIprops.toString())
processAction( "AddSDI", AddSDIprops );

      
// Copying the WorkItem from Parent Sample
def CopySDIDetailprops=[
    sdcid: "Sample",
    keyid1: AddSDIprops.get("newkeyid1"),
    copies: "1",
    sourcesdcid: "Sample",
    sourcekeyid1: strSamples[i],
    copydataset: "N",
    addnewonly: "N",
    copyspec: "Y",
    copyworkitem: "Y",
    applysourceworkitem: "Y"
]
logger.info( "Calling CopySDIDetail with properties: " + CopySDIDetailprops.toString())
processAction( "CopySDIDetail", CopySDIDetailprops);


// Add DataSet not having SDIWorkItem
String strQueryAddtionalDataSet="select '"+AddSDIprops.get("newkeyid1")+"' keyid1,paramlistid,paramlistversionid,variantid,dataset from sdidata where SOURCEWORKITEMID is null and keyid1='"+strSamples[i]+"' and dataset='1' ";
DataSet dsDataSetAdditionalDataSet= getSQLDataSet(strQueryAddtionalDataSet);
if(null==dsDataSetAdditionalDataSet){
    throw new SapphireException("Unable to create DataSet: dsDataSet");
}
if(dsDataSetAdditionalDataSet.getRowCount()>0){
  
  def AddDataSetprops=[
    sdcid: "Sample",
    keyid1: dsDataSetAdditionalDataSet.getColumnValues("keyid1",";"),
    keyid2: "",
    keyid3: "",
    paramlistid: dsDataSetAdditionalDataSet.getColumnValues("paramlistid",";"),
    paramlistversionid: dsDataSetAdditionalDataSet.getColumnValues("paramlistversionid",";"),
    variantid: dsDataSetAdditionalDataSet.getColumnValues("variantid",";"),
    dataset: dsDataSetAdditionalDataSet.getColumnValues("dataset",";"),
    available: "",
    addnewonly: "",
    workflowid: "",
    workflowversionid: "",
    workflowinstance: "",
    applylock: "",
    paramid: "",
    paramtype: "",
    replicateid: "",
    rsetid: "",
    propsmatch: "Y",
    auditreason: "",
    auditactivity: "",
    auditsignedflag: "",
    auditdt: "",
    param: "",
    matchusersequence: "",
    newds: "",
    adddataitems: "",
    trackitemid: "",
    scheduleplanid: "",
    scheduleplanitemid: "",
    s_qcbatchid: "",
    s_qcbatchitemid: "",
    createworksheet: "Y",
    formid: "",
    formversionid: "",
    securityuser: "",
    securitydepartment: "",
    newdatasetinstancexml: ""
]
logger.info( "Calling AddDataSet with properties: " + AddDataSetprops.toString())
processAction( "AddDataSet", AddDataSetprops );
  
}


// Find DataSet and copy them
String strDataSetQuery="select '"+AddSDIprops.get("newkeyid1")+"' keyid1,paramlistid,paramlistversionid,variantid,dataset,paramid,paramtype,max(dataset) dscount from sdidataitem where keyid1='"+strSamples[i]+"' group by keyid1,paramlistid,paramlistversionid,variantid,dataset,paramid,paramtype having max(dataset)>1";
DataSet dsDataSet= getSQLDataSet(strDataSetQuery);
if(null==dsDataSet){
    throw new SapphireException("Unable to create DataSet: dsDataSet");
}
String keyid1="";
String paramlistid="";
String paramlistversionid="";
String variantid="";
String dataset="";
String paramid="";
String paramtype="";
String replicateid="";

if(dsDataSet.getRowCount()>0){
    for(int k=0; k<dsDataSet.getRowCount() ;k++){
      for(int j=1;j<Integer.valueOf(dsDataSet.getValue(k,"dscount"));j++){
           if(keyid1==""){
               keyid1=dsDataSet.getValue(k,"keyid1");
           } else{
               keyid1=keyid1+";"+dsDataSet.getValue(k,"keyid1");
           }
           if(paramlistid==""){
                          paramlistid=dsDataSet.getValue(k,"paramlistid");
           } else{
                          paramlistid=paramlistid+";"+dsDataSet.getValue(k,"paramlistid");
           }
           if(variantid==""){
                          variantid=dsDataSet.getValue(k,"variantid");
           } else{
                          variantid=variantid+";"+dsDataSet.getValue(k,"variantid");
           }
           if(dataset==""){
                          dataset="1";
           } else{
                          dataset=dataset+";"+"1";
           }
           if(paramid==""){
                          paramid=dsDataSet.getValue(k,"paramid");
           } else{
                          paramid=paramid+";"+dsDataSet.getValue(k,"paramid");
           }
           if(paramtype==""){
                          paramtype=dsDataSet.getValue(k,"paramtype");
           } else{
                          paramtype=paramtype+";"+dsDataSet.getValue(k,"paramtype");
           }
           if(replicateid==""){
                          replicateid=dsDataSet.getValue(k,"replicateid");
           } else{
                          replicateid=replicateid+";"+dsDataSet.getValue(k,"replicateid");
           }
           if(paramlistversionid==""){
                          paramlistversionid=dsDataSet.getValue(k,"paramlistversionid");
           } else{
                          paramlistversionid=paramlistversionid+";"+dsDataSet.getValue(k,"paramlistversionid");
           }
           
           
   } 
}





def RemeasureDataSetprops=[
    sdcid: "Sample",
    keyid1: AddSDIprops.get("newkeyid1"),
    keyid2: "",
    keyid3: "",
    paramlistid: paramlistid,
    paramlistversionid: paramlistversionid,
    variantid: variantid,
    dataset: dataset,
    auditreason: "",
    newdsstatus: ""
]
logger.info( "Calling RemeasureDataSet with properties: " + RemeasureDataSetprops.toString())
processAction( "RemeasureDataSet", RemeasureDataSetprops );



}



// Find the Replicates if available for the Sample
String strReplicateQuery="select '"+AddSDIprops.get("newkeyid1")+"' keyid1,paramlistid,paramlistversionid,variantid,dataset,paramid,paramtype,max(replicateid)-1 repcount from sdidataitem where keyid1='"+strSamples[i]+"' group by keyid1,paramlistid,paramlistversionid,variantid,dataset,paramid,paramtype having max(replicateid)>1";
DataSet dsReplicate= getSQLDataSet(strReplicateQuery);
if(null==dsReplicate){
    throw new SapphireException("Unable to create DataSet: dsReplicate");
}

if(dsReplicate.getRowCount()>0){
    def AddReplicateprops=[
    sdcid: "Sample",
    keyid1: dsReplicate.getColumnValues("keyid1",";"),
    keyid2: "",
    keyid3: "",
    paramlistid: dsReplicate.getColumnValues("paramlistid",";"),
    paramlistversionid: dsReplicate.getColumnValues("paramlistversionid",";"),
    variantid: dsReplicate.getColumnValues("variantid",";"),
    dataset: dsReplicate.getColumnValues("dataset",";"),
    paramid: dsReplicate.getColumnValues("paramid",";"),
    paramtype: dsReplicate.getColumnValues("paramtype",";"),
    numreplicate: dsReplicate.getColumnValues("repcount",";"),
    propsmatch: "Y",
    applylock: "",
    separator: "",
    auditreason: "",
    auditactivity: "",
    auditsignedflag: "",
    auditdt: ""
]
logger.info( "Calling AddReplicate with properties: " + AddReplicateprops.toString())
processAction( "AddReplicate", AddReplicateprops );
}



// Reading the Batch Stage from Policy using Database Query
String PolicyQuery="select CASE WHEN REPLACE(REPLACE(xmltype(valuetree).extract('/propertytree/nodelist/node/propertylist/property/text()').getStringVal(),'<![CDATA[',''),']]>','') IS NULL THEN REPLACE(REPLACE(xmltype(valuetree).extract('/propertytree/propertydefaultlist/propertydefault/text()').getStringVal(),'<![CDATA[',''),']]>','') ELSE REPLACE(REPLACE(xmltype(valuetree).extract('/propertytree/nodelist/node/propertylist/property/text()').getStringVal(),'<![CDATA[',''),']]>','') END AS value from PROPERTYTREE where PROPERTYTREETYPE='Policy' AND PROPERTYTREEID    ='COAParentSamplePolicy'  ";
DataSet dsPolicyQuery= getSQLDataSet(PolicyQuery);
if(null==dsPolicyQuery){
    throw new SapphireException("Unable to create DataSet: dsPolicyQuery");
}
// Storing the Stage Name
String strStageName=dsPolicyQuery.getValue(0,"value");



// Find the Batch Stage Id from the Current Batch
String BatchStageQuery="select s_batchstage.s_batchstageid,s_batch.productid,s_batch.productversionid,s_batch.prodvariantid from s_batchstage,s_batch where s_batch.s_batchid=s_batchstage.batchid and s_batchstage.batchid='"+strChildBatchIds[i]+"' and s_batchstage.label='"+strStageName+"'";
DataSet dsBatchStage= getSQLDataSet(BatchStageQuery);
if(null==dsBatchStage){
    throw new SapphireException("Unable to create DataSet: dsBatchStage");
}
if(dsBatchStage.getRowCount()==0){
     throw new SapphireException("Batch Stage is blank in Policy/Not Available in Current Batch..!");
}
// Find the Batch Stage Id from the Current Batch Done



// Find the Spec from the Sampling Plan
String FindSpec="SELECT sitem.itemsdcid,sstage.label,sitem.itemkeyid1,sitem.itemkeyid2,sb.batchdesc,sb.productid,sb.productversionid,splan.s_samplingplanid,splan.s_samplingplanversionid, sdetail.levelid, sdetail.sourcelabel,  sdetail.templatekeyid1, sdetail.defaultdepartmentid FROM S_SAMPLINGPLAN SPLAN INNER JOIN S_PROCESSSTAGE SSTAGE ON SPLAN.S_SAMPLINGPLANID        =SSTAGE.S_SAMPLINGPLANID AND SPLAN.S_SAMPLINGPLANVERSIONID=SSTAGE.S_SAMPLINGPLANVERSIONID INNER JOIN S_SPDETAIL SDETAIL ON SDETAIL.S_SAMPLINGPLANID        =SSTAGE.S_SAMPLINGPLANID AND SDETAIL.S_SAMPLINGPLANVERSIONID=SSTAGE.S_SAMPLINGPLANVERSIONID AND SSTAGE.S_PROCESSSTAGEID        = SDETAIL.PROCESSSTAGEID INNER JOIN S_SPDETAILITEM SDETAILITEM ON SDETAILITEM.S_SAMPLINGPLANID        =SDETAIL.S_SAMPLINGPLANID AND SDETAILITEM.S_SAMPLINGPLANVERSIONID=SDETAIL.S_SAMPLINGPLANVERSIONID AND SDETAILITEM.S_SAMPLINGPLANDETAILNO =SDETAIL.S_SAMPLINGPLANDETAILNO INNER JOIN S_SPITEM SITEM ON SITEM.S_SAMPLINGPLANID        =SSTAGE.S_SAMPLINGPLANID AND SITEM.S_SAMPLINGPLANVERSIONID=SSTAGE.S_SAMPLINGPLANVERSIONID AND SITEM.S_SAMPLINGPLANITEMNO   =SDETAILITEM.S_SAMPLINGPLANITEMNO INNER JOIN S_BATCH SB ON SB.SAMPLINGPLANID        =SPLAN.S_SAMPLINGPLANID AND SB.SAMPLINGPLANVERSIONID=SPLAN.S_SAMPLINGPLANVERSIONID WHERE SB.S_BATCHID          = '"+strChildBatchIds[i]+"' AND label                   ='"+strStageName+"' AND SITEM.ITEMSDCID         ='SpecSDC' ORDER BY sitem.itemsdcid";

DataSet dsFindSpecs= getSQLDataSet(FindSpec);
if(null==dsFindSpecs){
    throw new SapphireException("Unable to create DataSet: dsFindSpecs");
}




// Find the Existing Spec if Present in the new Sample
String PresentSampleSpec="select specid,SPECVERSIONID from sdispec where keyid1='"+AddSDIprops.get("newkeyid1")+"'";
DataSet dsPresentSampleSepc=getSQLDataSet(PresentSampleSpec);
if(null==dsPresentSampleSepc){
    throw new SapphireException("Unable to create DataSet: dsPresentSampleSepc");
}
// Remove the Existing Spec only if Present
if(dsPresentSampleSepc.getRowCount()>0){
 def RemoveSDISpecprops=[
    sdcid: "Sample",
    keyid1: AddSDIprops.get("newkeyid1"),
    keyid2: "",
    keyid3: "",
    specid: dsPresentSampleSepc.getColumnValues("specid",";"),
    specversionid: dsPresentSampleSepc.getColumnValues("specversionid",";"),
    applylock: "",
    auditreason: "Event Plan: SycnCOASample",
    auditactivity: "",
    auditsignedflag: "",
    auditdt: ""
]
logger.info( "Calling RemoveSDISpec with properties: " + RemoveSDISpecprops.toString())
processAction( "RemoveSDISpec", RemoveSDISpecprops );
     
}

// Add new Spec to the Sample
if(dsFindSpecs.getRowCount()>0){
        def AddSDISpecprops=[
        sdcid: "Sample",
        keyid1: AddSDIprops.get("newkeyid1"),
        keyid2: "",
        keyid3: "",
        specid: dsFindSpecs.getColumnValues("itemkeyid1",";"),
        specversionid: dsFindSpecs.getColumnValues("itemkeyid2",";"),
        applylock: "",
        rsetid: "",
        auditreason: "Event Plan: SycnCOASample",
        auditactivity: "",
        auditsignedflag: "",
        auditdt: ""
    ]
    logger.info( "Calling AddSDISpec with properties: " + AddSDISpecprops.toString())
    processAction( "AddSDISpec", AddSDISpecprops );
 
}
// Add New Specs Done
String sourcespsourcelabel="";
String sourcesplevelid="";
String sourcespid="";
String sourcespversionid="";
if(dsFindSpecs.getRowCount()>0){
    sourcespsourcelabel=dsFindSpecs.getValue(0,"sourcelabel");
    sourcesplevelid=dsFindSpecs.getValue(0,"levelid");
    sourcespid=dsFindSpecs.getValue(0,"s_samplingplanid");
    sourcespversionid=dsFindSpecs.getValue(0,"s_samplingplanversionid"); 
}

// Update New Created Sample Information
def EditSDIprops=[
    sdcid: "Sample",
    keyid1: AddSDIprops.get("newkeyid1"),
    batchstageid: dsBatchStage.getValue(0,"s_batchstageid"),
    batchid: strChildBatchIds[i],
    productid: dsBatchStage.getValue(0,"productid"),
    productversionid: dsBatchStage.getValue(0,"productversionid"),
    samplestatus:"Reviewed", 
    sourcespsourcelabel:sourcespsourcelabel,
    sourcesplevelid:sourcesplevelid,
    sourcespid:sourcespid,
    sourcespversionid:sourcespversionid,
    auditactivity:"Event Plan: SycnCOASample",
    reviewdisposition: "Approved",
    disposalstatus: "Y",
    receiveddt: "N"
    
]
logger.info( "Calling EditSDI with properties: " + EditSDIprops.toString())
processAction( "EditSDI", EditSDIprops );


/*
Problem Desc : To resolve actual date problem while input as 'N'
Change Desc : Query Changed. Case statement added
Changed By : Kaushik Ghosh, CTS
Changed On : 20-OCT-2020
*/
// Identify the DataItem Value for Existiing Sample
String DataValue="select sdcid,'"+AddSDIprops.get("newkeyid1")+"' newsampleid,paramlistid,paramlistversionid,variantid,dataset,paramid,paramtype,replicateid,datatypes,enteredtext,transformdt,calcexcludeflag from sdidataitem where keyid1='"+strSamples[i]+"' ";
DataSet dsDataValue=getSQLDataSet(DataValue);

// For loop
if(null==dsPresentSampleSepc){
    throw new SapphireException("Unable to create DataSet: dsDataValue");
}

/*
Logic added to handle date input n or N
*/
dsDataValue.addColumn("finalText",DataSet.STRING);

for(int row=0;row<dsDataValue.getRowCount();row++){
	if (dsDataValue.getValue(row, "enteredtext") == 'n' || dsDataValue.getValue(row, "enteredtext") == 'N'){
		dsDataValue.setValue(row,"finalText",dsDataValue.getValue(row, "transformdt"));
	}
	else{
		dsDataValue.setValue(row,"finalText",dsDataValue.getValue(row, "enteredtext"));
	}
}

// Data Entry of new Sample based on Existing Sample
if(dsDataValue.getRowCount()>0){
                
				/*
				def EditDataItemprops=[
                    sdcid: "Sample",
                    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
                    keyid2: "",
                    keyid3: "",
                     paramlistid: dsDataValue.getColumnValues("paramlistid",";"),
                    paramlistversionid: dsDataValue.getColumnValues("paramlistversionid",";"),
                    variantid: dsDataValue.getColumnValues("variantid",";"),
                    dataset: dsDataValue.getColumnValues("dataset",";"),
                    paramid: dsDataValue.getColumnValues("paramid",";"),
                    paramtype: dsDataValue.getColumnValues("paramtype",";"),
                    replicateid: dsDataValue.getColumnValues("replicateid",";"),
                    auditreason: "",
                    enteredtext: dsDataValue.getColumnValues("textvalue",";"),
                    calcexcludeflag: dsDataValue.getColumnValues("calcexcludeflag",";"),
                    propsmatch: "",
                    auditactivity: "Event Plan: SycnCOASample",
                    auditsignedflag: "",
                    s_datasetstatus: "Released"
                ]
                logger.info( "Calling EditDataItem with properties: " + EditDataItemprops.toString())
                processAction( "EditDataItem", EditDataItemprops );   
*/
                  
                // Data Entry of new Sample based on Existing Sample
                def EnterDataItemprops=[
                    sdcid: "Sample",
                    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
                    keyid2: "",
                    keyid3: "",
                    paramlistid: dsDataValue.getColumnValues("paramlistid",";"),
                    paramlistversionid: dsDataValue.getColumnValues("paramlistversionid",";"),
                    variantid: dsDataValue.getColumnValues("variantid",";"),
                    dataset: dsDataValue.getColumnValues("dataset",";"),
                    paramid: dsDataValue.getColumnValues("paramid",";"),
                    paramtype: dsDataValue.getColumnValues("paramtype",";"),
                    replicateid: dsDataValue.getColumnValues("replicateid",";"),
                    enteredtext: dsDataValue.getColumnValues("finalText",";"),
                    releasedflag: "Y",
                    auditactivity: "Event Plan: SycnCOASample"
                   
                ]
                logger.info( "Calling EnterDataItem with properties: " + EnterDataItemprops.toString())
                processAction( "EnterDataItem", EnterDataItemprops ); 
                
                
                 
}



//def ReleaseDataSetprops=[
//    sdcid: "Sample",
//    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
//    keyid2: "",
//    keyid3: "",
//    paramlistid: dsDataValue.getColumnValues("paramlistid",";"),
//    paramlistversionid: dsDataValue.getColumnValues("paramlistversionid",";"),
//    variantid: dsDataValue.getColumnValues("variantid",";"),
//    dataset: dsDataValue.getColumnValues("dataset",";"),
//    releasedflag: "Y",
//    allowmandatorynulls: "",
//    eventnotify: "",
//    auditreason: "",
//    auditactivity: "",
//    auditsignedflag: "",
//    auditdt: ""
//]
//logger.info( "Calling ReleaseDataSet with properties: " + ReleaseDataSetprops.toString())
//processAction( "ReleaseDataSet", ReleaseDataSetprops );
//
//// Release Data Item
//def ReleaseDataItemprops=[
//    sdcid: "Sample",
//    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
//    keyid2: "",
//    keyid3: "",
//    paramlistid: dsDataValue.getColumnValues("paramlistid",";"),
//    paramlistversionid: dsDataValue.getColumnValues("paramlistversionid",";"),
//    variantid: dsDataValue.getColumnValues("variantid",";"),
//    dataset: dsDataValue.getColumnValues("dataset",";"),
//    paramid: dsDataValue.getColumnValues("paramid",";"),
//    paramtype: dsDataValue.getColumnValues("paramtype",";"),
//    replicateid: dsDataValue.getColumnValues("replicateid",";"),
//    eventnotify: "",
//    applylock: "",
//    allowmandatorynulls: "Y",
//    auditreason: "Event Plan: SycnCOASample",
//    auditactivity: "Event Plan: SycnCOASample"
//   
//]
//logger.info( "Calling ReleaseDataItem with properties: " + ReleaseDataItemprops.toString())
//processAction( "ReleaseDataItem", ReleaseDataItemprops );

//def UpdateDatasetStatusprops=[
//    sdcid: "Sample",
//    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
//    keyid2: "",
//    keyid3: "",
//    eventnotify: "",
//    auditreason: "Event Plan: SycnCOASample",
//    auditactivity: "",
//    auditsignedflag: ""
//]
//logger.info( "Calling UpdateDatasetStatus with properties: " + UpdateDatasetStatusprops.toString())
//processAction( "UpdateDatasetStatus", UpdateDatasetStatusprops );


// Update Parent Lot Sample Detail
def EditSDIpropsparentLot=[
    sdcid: "ParentLotSamples",
    keyid1: strIds[i],
    keyid2: "",
    keyid3: "",
    auditreason: "",
    eventnotify: "",
    applylock: "",
    auditactivity: "Event Plan: SycnCOASample",
    auditsignedflag: "",
    childsampleid: AddSDIprops.get("newkeyid1")
]
logger.info( "Calling EditSDI with properties: " + EditSDIpropsparentLot.toString())
processAction( "EditSDI", EditSDIpropsparentLot);
// Update Parent Lot Sample Detail Done


// Update Batch Stage Detail
def EditSDIpropsbatchstage=[
    sdcid: "LV_BatchStage",
    keyid1: dsBatchStage.getValue(0,"s_batchstageid"),
    batchstagestatus: "Released",
    auditactivity:"Event Plan: SycnCOASample"
    
]
logger.info( "Calling EditSDI with properties: " + EditSDIpropsbatchstage.toString())
processAction( "EditSDI", EditSDIpropsbatchstage);

// Retrieve the Work Item Information from the Existing Sample
String strQuery="select workitemid,workiteminstance,keyid1 from sdiworkitem where sdcid='Sample' and keyid1='"+AddSDIprops.get("newkeyid1")+"' ";
DataSet dsWorkItem=getSQLDataSet(strQuery);
if(null==dsWorkItem){
    throw new SapphireException("Unable to create DataSet: dsWorkItem");
}

// Sync SDI WorkItem Status for new Sample SDI
//def SyncSDIWIStatusprops=[
//    sdcid: "Sample",
//    keyid1: dsWorkItem.getColumnValues("keyid1",";"),
//    keyid2: "",
//    keyid3: "",
//    syncsdiworkitemgroupstatusonly: "",
//    workitemid: dsWorkItem.getColumnValues("workitemid",";"),
//    workiteminstance: dsWorkItem.getColumnValues("workiteminstance",";"),
//    groupid: "",
//    groupinstance: "",
//    auditreason: "",
//    auditactivity: "",
//    auditsignedflag: ""
//]
//logger.info( "Calling SyncSDIWIStatus with properties: " + SyncSDIWIStatusprops.toString())
//processAction( "SyncSDIWIStatus", SyncSDIWIStatusprops );


// Updating SDI WorkItem Status for new Sample
def EditSDIWorkItemprops=[
    sdcid: "Sample",
    keyid1: dsWorkItem.getColumnValues("keyid1",";"),
    keyid2: "",
    keyid3: "",
    workitemid: dsWorkItem.getColumnValues("workitemid",";"),
    workiteminstance: dsWorkItem.getColumnValues("workiteminstance",";"),
    applylock: "",
    propsmatch: "Y",
    auditreason: "Event Plan: SycnCOASample",
    auditactivity: "",
    auditsignedflag: "",
    workitemstatus: "Completed",
    formid: "",
    formversionid: ""
]
logger.info( "Calling EditSDIWorkItem with properties: " + EditSDIWorkItemprops.toString())
processAction( "EditSDIWorkItem", EditSDIWorkItemprops );



def ReleaseDataSetprops=[
    sdcid: "Sample",
    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
    keyid2: "",
    keyid3: "",
    paramlistid: dsDataValue.getColumnValues("paramlistid",";"),
    paramlistversionid: dsDataValue.getColumnValues("paramlistversionid",";"),
    variantid: dsDataValue.getColumnValues("variantid",";"),
    dataset: dsDataValue.getColumnValues("dataset",";"),
    releasedflag: "Y",
    allowmandatorynulls: "",
    eventnotify: "",
    auditreason: "",
    auditactivity: "",
    auditsignedflag: "",
    auditdt: ""
]
logger.info( "Calling ReleaseDataSet with properties: " + ReleaseDataSetprops.toString())
processAction( "ReleaseDataSet", ReleaseDataSetprops );

// Release Data Item
def ReleaseDataItemprops=[
    sdcid: "Sample",
    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
    keyid2: "",
    keyid3: "",
    paramlistid: dsDataValue.getColumnValues("paramlistid",";"),
    paramlistversionid: dsDataValue.getColumnValues("paramlistversionid",";"),
    variantid: dsDataValue.getColumnValues("variantid",";"),
    dataset: dsDataValue.getColumnValues("dataset",";"),
    paramid: dsDataValue.getColumnValues("paramid",";"),
    paramtype: dsDataValue.getColumnValues("paramtype",";"),
    replicateid: dsDataValue.getColumnValues("replicateid",";"),
    eventnotify: "",
    applylock: "",
    allowmandatorynulls: "Y",
    auditreason: "Event Plan: SycnCOASample",
    auditactivity: "Event Plan: SycnCOASample"
   
]
logger.info( "Calling ReleaseDataItem with properties: " + ReleaseDataItemprops.toString())
processAction( "ReleaseDataItem", ReleaseDataItemprops );

def UpdateDatasetStatusprops=[
    sdcid: "Sample",
    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
    keyid2: "",
    keyid3: "",
    eventnotify: "",
    auditreason: "Event Plan: SycnCOASample",
    auditactivity: "",
    auditsignedflag: ""
]
logger.info( "Calling UpdateDatasetStatus with properties: " + UpdateDatasetStatusprops.toString())
processAction( "UpdateDatasetStatus", UpdateDatasetStatusprops );







def ReleaseDataSetprops2=[
    sdcid: "Sample",
    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
    keyid2: "",
    keyid3: "",
    paramlistid: dsDataValue.getColumnValues("paramlistid",";"),
    paramlistversionid: dsDataValue.getColumnValues("paramlistversionid",";"),
    variantid: dsDataValue.getColumnValues("variantid",";"),
    dataset: dsDataValue.getColumnValues("dataset",";"),
    releasedflag: "Y",
    allowmandatorynulls: "",
    eventnotify: "",
    auditreason: "",
    auditactivity: "",
    auditsignedflag: "",
    auditdt: ""
]
logger.info( "Calling ReleaseDataSet with properties: " + ReleaseDataSetprops.toString())
processAction( "ReleaseDataSet", ReleaseDataSetprops2);

// Release Data Item
def ReleaseDataItemprops2=[
    sdcid: "Sample",
    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
    keyid2: "",
    keyid3: "",
    paramlistid: dsDataValue.getColumnValues("paramlistid",";"),
    paramlistversionid: dsDataValue.getColumnValues("paramlistversionid",";"),
    variantid: dsDataValue.getColumnValues("variantid",";"),
    dataset: dsDataValue.getColumnValues("dataset",";"),
    paramid: dsDataValue.getColumnValues("paramid",";"),
    paramtype: dsDataValue.getColumnValues("paramtype",";"),
    replicateid: dsDataValue.getColumnValues("replicateid",";"),
    eventnotify: "",
    applylock: "",
    allowmandatorynulls: "Y",
    auditreason: "Event Plan: SycnCOASample",
    auditactivity: "Event Plan: SycnCOASample"
   
]
logger.info( "Calling ReleaseDataItem with properties: " + ReleaseDataItemprops.toString())
processAction( "ReleaseDataItem", ReleaseDataItemprops2);

def UpdateDatasetStatusprops2=[
    sdcid: "Sample",
    keyid1: dsDataValue.getColumnValues("newsampleid",";"),
    keyid2: "",
    keyid3: "",
    eventnotify: "",
    auditreason: "Event Plan: SycnCOASample",
    auditactivity: "",
    auditsignedflag: ""
]
logger.info( "Calling UpdateDatasetStatus with properties: " + UpdateDatasetStatusprops.toString())
processAction( "UpdateDatasetStatus", UpdateDatasetStatusprops2);


    
//// Sync of SDI Data Set Status
//def SyncSDIDataSetStatusprops=[
//sdcid: "Sample",
//keyid1: dsDataValue.getColumnValues("newsampleid",";"),
//statuscolid: "samplestatus",
//auditreason: "Event Plan: SycnCOASample"
//]
//logger.info( "Calling SyncSDIDataSetStatus with properties: " + SyncSDIDataSetStatusprops.toString())
//processAction( "SyncSDIDataSetStatus", SyncSDIDataSetStatusprops );
//
//// Sync of SDI Data Set Status
//def SyncSDIDataSetStatusprops2=[
//sdcid: "Sample",
//keyid1: dsDataValue.getColumnValues("newsampleid",";"),
//statuscolid: "samplestatus",
//auditreason: "Event Plan: SycnCOASample"
//]
//logger.info( "Calling SyncSDIDataSetStatus with properties: " + SyncSDIDataSetStatusprops.toString())
//processAction( "SyncSDIDataSetStatus", SyncSDIDataSetStatusprops2);



  // Identify the DataItem Value for Existiing Sample
String strCancelDataSet="select '"+AddSDIprops.get("newkeyid1")+"' newsampleid,paramlistid,paramlistversionid,variantid,dataset,s_datasetstatus from sdidata where sdcid='Sample' and s_datasetstatus='Cancelled' and keyid1='"+strSamples[i]+"' ";
        DataSet dsCancelDataSet=getSQLDataSet(strCancelDataSet);
        if(null==dsCancelDataSet){
            throw new SapphireException("Unable to create DataSet: dsCancelDataSet");
        }
    if(dsCancelDataSet.getRowCount()>0){
             def CancelDataSetprops=[
                sdcid: "Sample",
                keyid1: dsCancelDataSet.getColumnValues("newsampleid",";"),
                keyid2: "",
                keyid3: "",
                paramlistid: dsCancelDataSet.getColumnValues("paramlistid",";"),
                paramlistversionid: dsCancelDataSet.getColumnValues("paramlistversionid",";"),
                variantid: dsCancelDataSet.getColumnValues("variantid",";"),
                dataset: dsCancelDataSet.getColumnValues("dataset",";"),
                auditreason: "Event Plan: SycnCOASample",
                cancelincompleteonly: "N"
            ]
            logger.info( "Calling CancelDataSet with properties: " + CancelDataSetprops.toString())
            processAction( "CancelDataSet", CancelDataSetprops );
    }      




}