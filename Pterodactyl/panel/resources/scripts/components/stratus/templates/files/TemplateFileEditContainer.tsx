import React, { useEffect, useState } from 'react';
import { useHistory, useLocation, useParams } from 'react-router-dom';
import PageContentBlock from '@/components/elements/PageContentBlock';
import SpinnerOverlay from '@/components/elements/SpinnerOverlay';
import CodemirrorEditor from '@/components/elements/CodemirrorEditor';
import Select from '@/components/elements/Select';
import Button from '@/components/elements/Button';
import modes from '@/modes';
import http from '@/api/http';
import { encodePathSegments, hashToPath } from '@/helpers';
import { dirname } from 'pathe';
import { ServerError } from '@/components/elements/ScreenBlock';
import FlashMessageRender from '@/components/FlashMessageRender';
import useFlash from '@/plugins/useFlash';

export default () => {
    const { id, action } = useParams<{ id: string; action: 'new' | 'edit' }>();
    const [loading, setLoading] = useState(action === 'edit');
    const [content, setContent] = useState('');
    const [error, setError] = useState('');
    const [mode, setMode] = useState('text/plain');
    const [filename, setFilename] = useState('');

    const history = useHistory();
    const { hash } = useLocation();
    const { addError, clearFlashes } = useFlash();

    let fetchFileContent: null | (() => Promise<string>) = null;

    useEffect(() => {
        if (action === 'new') {
            setLoading(false);
            return;
        }

        setError('');
        setLoading(true);
        const path = hashToPath(hash);
        setFilename(path.split('/').pop() || '');

        http.get(`/api/client/stratus/templates/${id}/files/contents`, { params: { file: path } })
            .then(({ data }) => setContent(data))
            .catch((err) => {
                console.error(err);
                setError(err.message || 'Failed to load file contents.');
            })
            .then(() => setLoading(false));
    }, [action, id, hash]);

    const save = (name?: string) => {
        if (!fetchFileContent) return;

        setLoading(true);
        clearFlashes('stratus:file-view');

        fetchFileContent()
            .then((content) => {
                const targetFile = name ? `${hashToPath(hash)}/${name}`.replace(/\/+/g, '/') : hashToPath(hash);
                return http.post(`/api/client/stratus/templates/${id}/files/write`, content, {
                    params: { file: targetFile },
                    headers: { 'Content-Type': 'text/plain' }
                }).then(() => targetFile);
            })
            .then((targetFile) => {
                if (action === 'new') {
                    history.push(`/stratus/templates/${id}/files/edit#/${encodePathSegments(targetFile)}`);
                }
            })
            .catch((err) => {
                console.error(err);
                addError({ message: err.message || 'Failed to save file.', key: 'stratus:file-view' });
            })
            .then(() => setLoading(false));
    };

    if (error) {
        return <ServerError message={error} onBack={() => history.goBack()} />;
    }

    return (
        <PageContentBlock title={action === 'new' ? 'Create File' : 'Edit File'}>
            <FlashMessageRender byKey={'stratus:file-view'} className={'mb-4'} />
            
            <div className={'bg-neutral-900 rounded shadow-lg overflow-hidden relative p-4'}>
                <SpinnerOverlay visible={loading} />

                {action === 'new' && (
                    <div className={'mb-4'}>
                        <label className={'block text-xs uppercase text-neutral-400 mb-1'}>File Name</label>
                        <input
                            type={'text'}
                            value={filename}
                            onChange={(e) => setFilename(e.target.value)}
                            placeholder={'e.g. server.properties'}
                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                        />
                    </div>
                )}

                <CodemirrorEditor
                    mode={mode}
                    filename={filename || hash.replace(/^#/, '')}
                    onModeChanged={setMode}
                    initialContent={content}
                    fetchContent={(value) => {
                        fetchFileContent = value;
                    }}
                    onContentSaved={() => {
                        if (action === 'new') {
                            if (!filename) {
                                addError({ message: 'Please provide a filename.', key: 'stratus:file-view' });
                                return;
                            }
                            save(filename);
                        } else {
                            save();
                        }
                    }}
                />

                <div className={'flex justify-between items-center mt-4'}>
                    <div className={'w-48 bg-neutral-800 rounded'}>
                        <Select value={mode} onChange={(e) => setMode(e.currentTarget.value)}>
                            {modes.map((m) => (
                                <option key={`${m.name}_${m.mime}`} value={m.mime}>
                                    {m.name}
                                </option>
                            ))}
                        </Select>
                    </div>

                    <div className={'flex space-x-2'}>
                        <Button 
                            color={'grey'} 
                            onClick={() => history.push(`/stratus/templates/${id}/files#${action === 'new' ? hashToPath(hash) : dirname(hashToPath(hash))}`)}
                        >
                            Cancel
                        </Button>
                        <Button 
                            onClick={() => {
                                if (action === 'new') {
                                    if (!filename) {
                                        addError({ message: 'Please provide a filename.', key: 'stratus:file-view' });
                                        return;
                                    }
                                    save(filename);
                                } else {
                                    save();
                                }
                            }}
                        >
                            {action === 'new' ? 'Create File' : 'Save File'}
                        </Button>
                    </div>
                </div>
            </div>
        </PageContentBlock>
    );
};
