#!/usr/bin/env python3
"""Prepare optional GitHub-secret signing without putting private key material in source/artifacts."""
import base64,json,os
from pathlib import Path
root=Path(os.environ['RUNNER_TEMP']);state=root/'devarix-signing-state.json'
data=os.environ.get('DEVARIX_KEYSTORE_B64','')
if not data:
    state.write_text(json.dumps({'mode':'ephemeral_debug','persistent_key_configured':False})+'\n')
    print('::warning::Persistent signing is not configured. This test APK uses a runner debug key; updates from other runs can require reinstalling the add-on.')
else:
    names=['DEVARIX_STORE_PASSWORD','DEVARIX_KEY_PASSWORD','DEVARIX_KEY_ALIAS']
    if any(not os.environ.get(name) or '\n' in os.environ[name] or '\r' in os.environ[name] for name in names):
        raise ValueError('All managed-signing secrets must be present and single-line')
    key=root/'devarix-managed.jks';key.write_bytes(base64.b64decode(''.join(data.split()),validate=True));key.chmod(0o600)
    with Path(os.environ['GITHUB_ENV']).open('a') as out:
        out.write('DEVARIX_KEYSTORE_FILE='+str(key)+'\n')
        for name in names:out.write(name+'='+os.environ[name]+'\n')
    state.write_text(json.dumps({'mode':'managed','persistent_key_configured':True})+'\n')
    print('Managed signing configured from repository secrets. No key material is included in artifacts.')
