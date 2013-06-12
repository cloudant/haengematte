import Data.Char (intToDigit)
import Network.HTTP
import Network.HTTP.Auth
import Network.Stream (ConnError)
import Network.URI
import System.Environment (getArgs)
import System.Exit (exitFailure)
import System.IO
import Data.Maybe (fromJust)
import Debug.Trace (trace)
import System.Environment (getEnv)
import Text.JSON
import Text.JSON.Types

data Person = Person String Int -- name, age

loop = loop

instance JSON Person where
 readJSON js = Ok (Person "john" 35)
 showJSON p = loop
 readJSONs js = loop
 showJSONs ps = loop

content = "{\"name\": \"john\", \"age\": 35}"

main = 
  do
  user <- getEnv "user"
  pass <- getEnv "pass"
  db <- getEnv "db"
  let baseUriStr = "http://kimstebel.cloudant.com/" ++ db
  let baseUri = fromJust (parseURI baseUriStr)
  let creds = AuthBasic "realm" user pass
  let baseCreds = creds baseUri
  postResp <- post baseUri baseCreds content
  let js = (decode postResp)::(Result JSValue)
  let id = (case js of
                 Ok (JSObject obj) -> grab obj "id")
  putStrLn ("The new document's id is " ++ id ++ ".")
  let idUriStr = baseUriStr ++ "/" ++ id
  let idUri = fromJust (parseURI idUriStr)
  let idCreds = creds idUri
  getResp <- get idUri idCreds
  let Ok doc1 = (decode getResp)::(Result JSValue)
  let rev1 = (case doc1 of
                   JSObject obj -> grab obj "_rev")
  putStrLn ("The first revision is " ++ rev1 ++ ".")
  let fields = fromJSObject (case doc1 of JSObject obj -> obj)
  let newFields = ("_rev", JSString (toJSString rev1)):fields
  putResp <- put idUri idCreds (jsToString (toJSObject newFields))
  let Ok putRespJson = (decode putResp)::(Result JSValue)
  let rev2 = (case putRespJson of
                   JSObject obj -> grab obj "rev")
  putStrLn ("The second revision is " ++ rev2 ++ ".")
  let deleteUri = fromJust (parseURI (idUriStr ++ "?rev=" ++ rev2))
  deleteResp <- delete deleteUri idCreds
  putStrLn ("We will now delete the document. The HTTP response is: " ++ deleteResp)
  
jsToString jso = showJSObject jso ""
  
grab o s = case get_field o s of
                Nothing            -> error "Invalid field " ++ show s
                Just (JSString s') -> fromJSString s'
  
err :: String -> IO a
err msg = do
	  hPutStrLn stderr msg
	  exitFailure

delete :: URI -> Authority -> IO String
delete uri creds = doReq (createReq DELETE uri creds "")

put :: URI -> Authority -> String -> IO String
put uri creds body = doReq (createReq PUT uri creds body)

get :: URI -> Authority -> IO String
get uri creds = doReq (createReq GET uri creds "")
    
post :: URI -> Authority -> String -> IO String
post uri creds body = doReq (createReq POST uri creds body)

doReq :: Request_String -> IO String
doReq req = 
  do
  eresp <- simpleHTTP req
  resp <- handleE (err . show) eresp
  case rspCode resp of
    (2,_,_) -> return (rspBody resp)
    _ -> err (httpError resp)
  where
  showRspCode (a,b,c) = map intToDigit [a,b,c]
  httpError resp = showRspCode (rspCode resp) ++ " " ++ rspReason resp ++ " " ++ rspBody resp
 
createReq :: RequestMethod -> URI -> Authority -> String -> Request_String
createReq method uri creds content = req { rqHeaders = contentHeaders ++ authHdr:rqHeaders req } where
 contentHeaders = case content of
   "" -> []
   c -> ctHdr:clHdr:[]
 authHdr = (mkHeader HdrAuthorization authString)
 clHdr = (mkHeader HdrContentLength (show (length content)))
 req = Request { rqURI = uri, rqMethod = method, rqHeaders = [], rqBody = content }
 authString = withAuthority creds req
 ctHdr = (mkHeader HdrContentType "application/json")
  
handleE :: Monad m => (ConnError -> m a) -> Either ConnError a -> m a
handleE h (Left e) = h e
handleE _ (Right v) = return v
