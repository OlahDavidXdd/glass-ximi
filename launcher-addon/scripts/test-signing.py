#!/usr/bin/env python3
import base64,json,os,subprocess,sys,tempfile,unittest
from pathlib import Path
SCRIPT=Path(__file__).with_name('configure-signing.py')
class SigningConfigTest(unittest.TestCase):
    def run_config(self,path,**values):
        env={'PATH':os.environ['PATH'],'RUNNER_TEMP':str(path),'GITHUB_ENV':str(path/'env'),**values}
        return subprocess.run([sys.executable,str(SCRIPT)],env=env,text=True,capture_output=True)
    def test_missing_key_is_explicit_debug_mode(self):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d);result=self.run_config(path);self.assertEqual(0,result.returncode);self.assertIn('runner debug key',result.stdout);self.assertFalse(json.loads((path/'devarix-signing-state.json').read_text())['persistent_key_configured']);self.assertFalse((path/'devarix-managed.jks').exists())
    def test_managed_bytes_are_private_and_not_logged(self):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d);data=b'test-fixture-not-a-real-signing-key';values=dict(DEVARIX_KEYSTORE_B64='\n'+base64.b64encode(data).decode()[:8]+'\n'+base64.b64encode(data).decode()[8:]+'\n',DEVARIX_STORE_PASSWORD='fixture-store-pass',DEVARIX_KEY_PASSWORD='fixture-key-pass',DEVARIX_KEY_ALIAS='fixture')
            result=self.run_config(path,**values);self.assertEqual(0,result.returncode,result.stderr);self.assertEqual(data,(path/'devarix-managed.jks').read_bytes());self.assertEqual(0o600,(path/'devarix-managed.jks').stat().st_mode&0o777)
            self.assertIn('DEVARIX_KEYSTORE_FILE=',(path/'env').read_text());self.assertNotIn('fixture-store-pass',result.stdout);self.assertNotIn(data.decode(),result.stdout)
    def test_incomplete_configuration_fails_before_export(self):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d);result=self.run_config(path,DEVARIX_KEYSTORE_B64='dGVzdA==');self.assertNotEqual(0,result.returncode);self.assertFalse((path/'env').exists());self.assertFalse((path/'devarix-managed.jks').exists())
    def test_invalid_base64_fails_before_export(self):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d);result=self.run_config(path,DEVARIX_KEYSTORE_B64='!not-base64!',DEVARIX_STORE_PASSWORD='x',DEVARIX_KEY_PASSWORD='y',DEVARIX_KEY_ALIAS='z');self.assertNotEqual(0,result.returncode);self.assertFalse((path/'env').exists())
if __name__=='__main__':unittest.main()
