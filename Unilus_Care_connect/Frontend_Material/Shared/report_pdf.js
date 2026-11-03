function parseReportCsv(csv){
 const rows=[],row=[];
 let cell='',quoted=false;
 for(let i=0;i<csv.length;i++){
  const char=csv[i];
  if(quoted){
   if(char==='"'&&csv[i+1]==='"'){cell+='"';i++}
   else if(char==='"')quoted=false;
   else cell+=char;
  }else if(char==='"')quoted=true;
  else if(char===','){row.push(cell);cell=''}
  else if(char==='\n'||char==='\r'){
   row.push(cell);cell='';
   if(row.some(value=>value.trim()))rows.push(row.splice(0));
   if(char==='\r'&&csv[i+1]==='\n')i++;
  }else cell+=char;
 }
 row.push(cell);
 if(row.some(value=>value.trim()))rows.push(row);
 if(quoted)throw new Error('The report CSV contains an unfinished quoted field.');
 return rows;
}

function reportPdfText(value){
 return String(value??'').replace(/\s+/g,' ').trim().replace(/[^\x20-\x7E]/g,'?');
}

function reportPdfEscape(value){
 return reportPdfText(value).replace(/[\\()]/g,char=>`\\${char}`);
}

function buildReportPdf(csv){
 const rows=parseReportCsv(csv);
 if(!rows.length)throw new Error('The backend returned an empty report.');
 const headers=rows[0].map(reportPdfText);
 const privateField=/^(?:(?:student|user|account|appointment|counsell?or|full)?(?:name|email|phone|mobile|id|tag|number)|address|dob|dateofbirth)$/i;
 const columns=headers.map((header,index)=>({header,index})).filter(column=>!privateField.test(column.header.replace(/[^a-z0-9]/gi,'')));
 const lines=[
  'UNILUS CareConnect - Anonymised Report',
  `Generated: ${new Date().toLocaleString()}`,
  'Aggregated report data. Direct identifying fields are excluded.',
  ''
 ];
 if(rows.length===1)lines.push('No report records were returned.');
 else if(!columns.length)lines.push('No non-identifying report columns were available.');
 else{
  lines.push(columns.map(column=>column.header).join(' | '),'='.repeat(72));
  rows.slice(1).forEach((row,index)=>{
   const countColumn=columns.find(column=>/^(n|count|studentcount|students|total|totalstudents|records|casecount|sessioncount)$/i.test(column.header.replace(/[^a-z0-9]/gi,'')));
   const count=countColumn?Number(row[countColumn.index]):NaN;
   const values=columns.map(column=>`${column.header}: ${countColumn===column&&Number.isFinite(count)&&count<5?'Hidden (n<5)':reportPdfText(row[column.index]||'')}`);
   const record=`Record ${index+1} - ${values.join(' | ')}`;
   for(let start=0;start<record.length;start+=92)lines.push(record.slice(start,start+92));
   lines.push('');
  });
 }

 const pageSize=45,pages=[];
 for(let i=0;i<lines.length;i+=pageSize)pages.push(lines.slice(i,i+pageSize));
 const objects=[];
 objects[1]='<< /Type /Catalog /Pages 2 0 R >>';
 objects[3]='<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>';
 const pageRefs=pages.map((_,index)=>`${4+index*2} 0 R`).join(' ');
 objects[2]=`<< /Type /Pages /Kids [${pageRefs}] /Count ${pages.length} >>`;
 pages.forEach((page,index)=>{
  const pageId=4+index*2,contentId=pageId+1;
  const commands=page.map((line,lineIndex)=>`BT /F1 9 Tf 1 0 0 1 42 ${790-lineIndex*16} Tm (${reportPdfEscape(line)}) Tj ET`).join('\n');
  objects[pageId]=`<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Resources << /Font << /F1 3 0 R >> >> /Contents ${contentId} 0 R >>`;
  objects[contentId]=`<< /Length ${commands.length} >>\nstream\n${commands}\nendstream`;
 });

 let pdf='%PDF-1.4\n',offsets=[0];
 for(let id=1;id<objects.length;id++){
  offsets[id]=pdf.length;
  pdf+=`${id} 0 obj\n${objects[id]}\nendobj\n`;
 }
 const xrefOffset=pdf.length;
 pdf+=`xref\n0 ${objects.length}\n0000000000 65535 f \n`;
 for(let id=1;id<objects.length;id++)pdf+=`${String(offsets[id]).padStart(10,'0')} 00000 n \n`;
 pdf+=`trailer\n<< /Size ${objects.length} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF`;
 return new Blob([pdf],{type:'application/pdf'});
}

async function downloadAnonymisedReportPdf(){
 if(typeof API==='undefined'||typeof API.reportCsv!=='function')throw new Error('The report API is not available.');
 const csv=await API.reportCsv();
 if(typeof csv!=='string')throw new Error('The backend did not return a CSV report.');
 const pdf=buildReportPdf(csv),url=URL.createObjectURL(pdf),link=document.createElement('a');
 link.href=url;
 link.download=`careconnect-anonymised-report-${new Date().toISOString().slice(0,10)}.pdf`;
 document.body.appendChild(link);
 link.click();
 link.remove();
 setTimeout(()=>URL.revokeObjectURL(url),1000);
}
