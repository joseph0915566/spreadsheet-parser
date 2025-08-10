Description
This application is a 3-tier application. User can access via https://<company-domain>/company-matching
and you will be redirected to the Upload Page where you can choose spreadsheet file to uploaded and parsed.
You will be notified via email when the parsing has finished where you will be given an S3 download URL.

Your file will be uploaded in chunks directly to S3 from your device. The file will be split into chunks and each
chunk will be uploaded concurrently with 3 retries each chunk. Once upload is complete, server will immediately
start parsing the rows concurrently using S3 SELECT to speed up processing. Once all parsing is done, server will
prepare an S3 download URL and email that to you.
