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
import tw from 'twin.macro';
import ErrorBoundary from '@/components/elements/ErrorBoundary';
import Modal from '@/components/elements/Modal';
import { Form, Formik, FormikHelpers } from 'formik';
import { object, string } from 'yup';
import Field from '@/components/elements/Field';

const joinPaths = (a: string, b: string) => (a === '/' ? '/' + b : a + '/' + b).replace(/\/+/g, '/');

export default () => {
    const { id, action } = useParams<{ id: string; action: 'new' | 'edit' }>();
    const [loading, setLoading] = useState(action === 'edit');
    const [content, setContent] = useState('');
    const [error, setError] = useState('');
    const [mode, setMode] = useState('text/plain');
    const [filename, setFilename] = useState('');
    const [modalVisible, setModalVisible] = useState(false);

    const history = useHistory();
    const { hash } = useLocation();
    const { addError, clearFlashes } = useFlash();

    let fetchFileContent: null | (() => Promise<string>) = null;

    useEffect(() => {
        if (action === 'new') {
            setLoading(false);
            setFilename('');
            setContent('');
            return;
        }

        setError('');
        setLoading(true);
        const path = hashToPath(hash);
        setFilename(path.split('/').pop() || '');

        http.get(`/api/client/stratus/templates/${id}/files/contents`, {
                params: { file: path },
                transformResponse: (res) => res,
                responseType: 'text',
            })
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
                const targetFile = name ? joinPaths(hashToPath(hash), name) : hashToPath(hash);
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

    const breadcrumbs = (): { name: string; path?: string }[] => {
        const path = action === 'new' ? hashToPath(hash) : dirname(hashToPath(hash));
        return path
            .split('/')
            .filter((dir) => !!dir)
            .map((dir, index, dirs) => {
                const p = `/${dirs.slice(0, index + 1).join('/')}`;
                return { name: dir, path: p };
            });
    };

    if (error) {
        return <ServerError message={error} onBack={() => history.goBack()} />;
    }

    return (
        <PageContentBlock>
            <FlashMessageRender byKey={'stratus:file-view'} css={tw`mb-4`} />
            
            <ErrorBoundary>
                <div css={tw`flex flex-grow-0 items-center text-sm text-neutral-500 overflow-x-hidden mb-4 py-2`}>
                    /<span css={tw`px-1 text-neutral-300`}>home</span>/
                    <a
                        href={`/stratus/templates/${id}#${action === 'new' ? hashToPath(hash) : dirname(hashToPath(hash))}`}
                        css={tw`px-1 text-neutral-200 no-underline hover:text-neutral-100`}
                    >
                        template
                    </a>
                    /
                    {breadcrumbs().map((crumb, index) => (
                        <React.Fragment key={index}>
                            <a
                                href={`/stratus/templates/${id}#${crumb.path}`}
                                css={tw`px-1 text-neutral-200 no-underline hover:text-neutral-100`}
                            >
                                {crumb.name}
                            </a>
                            /
                        </React.Fragment>
                    ))}
                    {action !== 'new' && (
                        <span css={tw`px-1 text-neutral-300`}>{filename}</span>
                    )}
                </div>
            </ErrorBoundary>

            <Modal
                visible={modalVisible}
                onDismissed={() => setModalVisible(false)}
            >
                <Formik
                    onSubmit={(values, { setSubmitting }) => {
                        setModalVisible(false);
                        save(values.fileName);
                        setSubmitting(false);
                    }}
                    initialValues={{ fileName: '' }}
                    validationSchema={object().shape({
                        fileName: string().required().min(1),
                    })}
                >
                    {({ resetForm }) => (
                        <Form css={tw`m-0`}>
                            <Field
                                id={'fileName'}
                                name={'fileName'}
                                label={'File Name'}
                                description={'Enter the name that this file should be saved as.'}
                                autoFocus
                            />
                            <div css={tw`mt-6 text-right`}>
                                <Button type={'submit'}>Create File</Button>
                            </div>
                        </Form>
                    )}
                </Formik>
            </Modal>

            <div className={'relative'}>
                <SpinnerOverlay visible={loading} />

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
                            setModalVisible(true);
                        } else {
                            save();
                        }
                    }}
                />
            </div>

            <div css={tw`flex justify-end mt-4`}>
                <div css={tw`flex-1 sm:flex-none rounded bg-neutral-900 mr-4 w-48`}>
                    <Select value={mode} onChange={(e) => setMode(e.currentTarget.value)}>
                        {modes.map((m) => (
                            <option key={`${m.name}_${m.mime}`} value={m.mime}>
                                {m.name}
                            </option>
                        ))}
                    </Select>
                </div>
                
                <Button 
                    color={'grey'} 
                    onClick={() => history.push(`/stratus/templates/${id}#${action === 'new' ? hashToPath(hash) : dirname(hashToPath(hash))}`)}
                    css={tw`mr-2`}
                >
                    Cancel
                </Button>
                
                {action === 'new' ? (
                    <Button onClick={() => setModalVisible(true)}>
                        Create File
                    </Button>
                ) : (
                    <Button onClick={() => save()}>
                        Save Content
                    </Button>
                )}
            </div>
        </PageContentBlock>
    );
};
