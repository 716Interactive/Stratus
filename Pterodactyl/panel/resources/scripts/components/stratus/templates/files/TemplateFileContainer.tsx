import React, { useEffect, useState, useRef } from 'react';
import { useLocation, useParams, Link } from 'react-router-dom';
import useSWR from 'swr';
import getTemplateFiles from '@/api/stratus/templates/getTemplateFiles';
import Spinner from '@/components/elements/Spinner';
import tw from 'twin.macro';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { 
    faFolder, 
    faFileAlt, 
    faFileArchive, 
    faTrashAlt, 
    faUpload, 
    faEllipsisH,
    faPencilAlt,
    faLevelUpAlt,
    faBoxOpen,
    faFileDownload,
    faCloudUploadAlt
} from '@fortawesome/free-solid-svg-icons';
import { bytesToString } from '@/lib/formatters';
import { differenceInHours, format, formatDistanceToNow } from 'date-fns';
import http from '@/api/http';
import Modal from '@/components/elements/Modal';
import { Button } from '@/components/elements/button/index';
import SpinnerOverlay from '@/components/elements/SpinnerOverlay';
import ErrorBoundary from '@/components/elements/ErrorBoundary';
import DropdownMenu from '@/components/elements/DropdownMenu';
import { FileObject } from '@/api/server/files/loadDirectory';
import styles from '@/components/server/files/style.module.css';
import Portal from '@/components/elements/Portal';
import Fade from '@/components/elements/Fade';
import { Dialog } from '@/components/elements/dialog';
import { CSSTransition } from 'react-transition-group';
import { ModalMask } from '@/components/elements/Modal';
import { FileActionCheckbox } from '@/components/server/files/SelectFileCheckbox';
import Field from '@/components/elements/Field';
import { Form, Formik, FormikHelpers } from 'formik';
import { object, string } from 'yup';
import Code from '@/components/elements/Code';

const joinPaths = (a: string, b: string) => (a === '/' ? '/' + b : a + '/' + b).replace(/\/+/g, '/');

const sortFiles = (files: FileObject[]): FileObject[] => {
    const sortedFiles: FileObject[] = files
        .sort((a, b) => a.name.localeCompare(b.name))
        .sort((a, b) => (a.isFile === b.isFile ? 0 : a.isFile ? 1 : -1));
    return sortedFiles;
};

function isFileOrDirectory(event: DragEvent): boolean {
    if (!event.dataTransfer?.types) {
        return false;
    }
    return event.dataTransfer.types.some((value) => value.toLowerCase() === 'files');
}

export default () => {
    const { id } = useParams<{ id: string }>();
    const { hash } = useLocation();
    const [directory, setDirectory] = useState(hash.replace('#', '') || '/');
    const [loading, setLoading] = useState(false);
    const [selectedFiles, setSelectedFiles] = useState<string[]>([]);
    
    // Drag and drop overlay state
    const [dragOverActive, setDragOverActive] = useState(false);

    // Create folder modal state
    const [folderModalVisible, setFolderModalVisible] = useState(false);

    // Rename Modal State
    const [renameModalVisible, setRenameModalVisible] = useState(false);
    const [renameTarget, setRenameTarget] = useState<FileObject | null>(null);

    // Move Modal State
    const [moveModalVisible, setMoveModalVisible] = useState(false);
    const [moveTarget, setMoveTarget] = useState<FileObject | null>(null);

    // Dialog.Confirm Deletion States
    const [fileToDelete, setFileToDelete] = useState<string | null>(null);
    const [showMassDeleteConfirm, setShowMassDeleteConfirm] = useState(false);

    const fileInputRef = useRef<HTMLInputElement>(null);

    const dropdownRefs = useRef<Record<string, DropdownMenu | null>>({});

    const { data: files, error, mutate } = useSWR(
        [`/stratus/templates/${id}/files/list`, directory],
        () => getTemplateFiles(id, directory)
    );

    // Drag-and-drop listeners
    useEffect(() => {
        const handleDragEnter = (e: DragEvent) => {
            e.preventDefault();
            e.stopPropagation();
            if (isFileOrDirectory(e)) {
                setDragOverActive(true);
            }
        };

        const handleDragLeaveOrKey = () => setDragOverActive(false);

        window.addEventListener('dragenter', handleDragEnter, { capture: true });
        window.addEventListener('dragexit', handleDragLeaveOrKey, { capture: true });
        window.addEventListener('keydown', handleDragLeaveOrKey);

        return () => {
            window.removeEventListener('dragenter', handleDragEnter, { capture: true });
            window.removeEventListener('dragexit', handleDragLeaveOrKey, { capture: true });
            window.removeEventListener('keydown', handleDragLeaveOrKey);
        };
    }, []);

    useEffect(() => {
        setDirectory(hash.replace('#', '') || '/');
        setSelectedFiles([]);
    }, [hash]);

    const handleUploadClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileSubmission = (fileList: FileList) => {
        const filesToUpload = Array.from(fileList);
        if (filesToUpload.length === 0) return;

        setLoading(true);
        const promises = filesToUpload.map(file => {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('directory', directory);
            formData.append('extract', 'true');

            return http.post(`/api/client/stratus/templates/${id}/files/upload`, formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
        });

        Promise.all(promises)
            .then(() => mutate())
            .catch(err => {
                console.error(err);
                alert('Upload failed: ' + (err.message || 'Unknown error'));
            })
            .then(() => {
                setLoading(false);
                if (fileInputRef.current) fileInputRef.current.value = '';
            });
    };

    const handleUploadChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (!e.target.files) return;
        handleFileSubmission(e.target.files);
    };

    const handleCreateFolder = ({ directoryName }: { directoryName: string }, { setSubmitting }: FormikHelpers<{ directoryName: string }>) => {
        if (!directoryName.trim()) return;

        const targetPath = joinPaths(directory, `${directoryName.trim()}/.keep`);

        http.post(`/api/client/stratus/templates/${id}/files/write`, '', {
            params: { file: targetPath },
            headers: { 'Content-Type': 'text/plain' }
        })
        .then(() => {
            setFolderModalVisible(false);
            mutate();
        })
        .catch(err => {
            console.error(err);
            alert('Failed to create folder: ' + err.message);
            setSubmitting(false);
        });
    };

    const handleRename = ({ name }: { name: string }, { setSubmitting }: FormikHelpers<{ name: string }>) => {
        if (!renameTarget || !name.trim()) return;

        const fromPath = joinPaths(directory, renameTarget.name);
        const toPath = joinPaths(directory, name.trim());

        http.post(`/api/client/stratus/templates/${id}/files/rename`, { from: fromPath, to: toPath })
        .then(() => {
            setRenameModalVisible(false);
            setRenameTarget(null);
            mutate();
        })
        .catch(err => {
            console.error(err);
            alert('Rename failed: ' + err.message);
            setSubmitting(false);
        });
    };

    const handleMove = ({ name }: { name: string }, { setSubmitting }: FormikHelpers<{ name: string }>) => {
        if (!moveTarget || !name.trim()) return;

        const fromPath = joinPaths(directory, moveTarget.name);
        const toPath = joinPaths(name.trim(), moveTarget.name);

        http.post(`/api/client/stratus/templates/${id}/files/rename`, { from: fromPath, to: toPath })
        .then(() => {
            setMoveModalVisible(false);
            setMoveTarget(null);
            mutate();
        })
        .catch(err => {
            console.error(err);
            alert('Move failed: ' + err.message);
            setSubmitting(false);
        });
    };

    const handleArchive = (fileName: string) => {
        setLoading(true);
        http.post(`/api/client/stratus/templates/${id}/files/compress`, { directory, files: [fileName] })
        .then(() => mutate())
        .catch(err => {
            console.error(err);
            alert('Compression failed: ' + err.message);
        })
        .then(() => setLoading(false));
    };

    const handleUnarchive = (fileName: string) => {
        setLoading(true);
        const filePath = joinPaths(directory, fileName);
        http.post(`/api/client/stratus/templates/${id}/files/decompress`, { file: filePath })
        .then(() => mutate())
        .catch(err => {
            console.error(err);
            alert('Decompression failed: ' + err.message);
        })
        .then(() => setLoading(false));
    };

    const handleDownload = (fileName: string) => {
        const filePath = joinPaths(directory, fileName);
        window.location.href = `/api/client/stratus/templates/${id}/files/download?file=${encodeURIComponent(filePath)}`;
    };

    const executeDelete = (name: string) => {
        setLoading(true);
        const targetPath = joinPaths(directory, name);

        http.post(`/api/client/stratus/templates/${id}/files/delete`, { files: [targetPath] })
        .then(() => {
            setSelectedFiles(prev => prev.filter(n => n !== name));
            mutate();
        })
        .catch(err => {
            console.error(err);
            alert('Delete failed: ' + err.message);
        })
        .then(() => setLoading(false));
    };

    const executeMassDelete = () => {
        setLoading(true);
        const targetPaths = selectedFiles.map(name => joinPaths(directory, name));

        http.post(`/api/client/stratus/templates/${id}/files/delete`, { files: targetPaths })
        .then(() => {
            setSelectedFiles([]);
            mutate();
        })
        .catch(err => {
            console.error(err);
            alert('Delete failed: ' + err.message);
        })
        .then(() => setLoading(false));
    };

    const handleMassCompress = () => {
        setLoading(true);
        http.post(`/api/client/stratus/templates/${id}/files/compress`, { directory, files: selectedFiles })
        .then(() => {
            setSelectedFiles([]);
            mutate();
        })
        .catch(err => {
            console.error(err);
            alert('Compress failed: ' + err.message);
        })
        .then(() => setLoading(false));
    };

    const breadcrumbs = (): { name: string; path?: string }[] =>
        directory
            .split('/')
            .filter((dir) => !!dir)
            .map((dir, index, dirs) => {
                if (index === dirs.length - 1) {
                    return { name: dir };
                }
                const path = `/${dirs.slice(0, index + 1).join('/')}`;
                return { name: dir, path };
            });

    if (error) return <div>Failed to load template files.</div>;

    const sortedFilesList = files ? sortFiles(files) : [];

    return (
        <div className={'relative'}>
            <SpinnerOverlay visible={loading} />

            {/* Drag and Drop Overlay Portal */}
            <Portal>
                <Fade appear in={dragOverActive} timeout={75} key={'upload_modal_mask'} unmountOnExit>
                    <ModalMask
                        onClick={() => setDragOverActive(false)}
                        onDragOver={(e) => e.preventDefault()}
                        onDrop={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            setDragOverActive(false);
                            if (e.dataTransfer?.files.length) {
                                handleFileSubmission(e.dataTransfer.files);
                            }
                        }}
                    >
                        <div className={'w-full flex items-center justify-center pointer-events-none'}>
                            <div className={'flex items-center space-x-4 bg-black w-full ring-4 ring-cyan-500 ring-opacity-60 rounded p-6 mx-10 max-w-sm border border-cyan-800'}>
                                <FontAwesomeIcon icon={faCloudUploadAlt} className={'text-cyan-400 text-3xl mr-4'} />
                                <p className={'font-header flex-1 text-lg text-neutral-100 text-center'}>
                                    Drag and drop files to upload.
                                </p>
                            </div>
                        </div>
                    </ModalMask>
                </Fade>
            </Portal>

            {/* Single File Deletion Confirmation */}
            <Dialog.Confirm
                open={fileToDelete !== null}
                onClose={() => setFileToDelete(null)}
                title={'Delete File / Directory'}
                confirm={'Delete'}
                onConfirmed={() => {
                    if (fileToDelete) {
                        executeDelete(fileToDelete);
                        setFileToDelete(null);
                    }
                }}
            >
                Are you sure you want to delete&nbsp;
                <span className={'font-semibold text-gray-50'}>{fileToDelete}</span>? This action is permanent and cannot be undone.
            </Dialog.Confirm>

            {/* Mass Deletion Confirmation */}
            <Dialog.Confirm
                open={showMassDeleteConfirm}
                onClose={() => setShowMassDeleteConfirm(false)}
                title={'Delete Files'}
                confirm={'Delete'}
                onConfirmed={() => {
                    executeMassDelete();
                    setShowMassDeleteConfirm(false);
                }}
            >
                <p className={'mb-2'}>
                    Are you sure you want to delete&nbsp;
                    <span className={'font-semibold text-gray-50'}>{selectedFiles.length} files</span>? This is a permanent action and the files cannot be recovered.
                </p>
                {selectedFiles.slice(0, 15).map((file) => (
                    <li key={file}>{file}</li>
                ))}
                {selectedFiles.length > 15 && <li>and {selectedFiles.length - 15} others</li>}
            </Dialog.Confirm>

            {/* Create Folder Modal */}
            <Modal visible={folderModalVisible} onDismissed={() => setFolderModalVisible(false)}>
                <Formik
                    onSubmit={handleCreateFolder}
                    initialValues={{ directoryName: '' }}
                    validationSchema={object().shape({
                        directoryName: string().required('A valid directory name must be provided.'),
                    })}
                >
                    {({ isSubmitting, submitForm, values }) => (
                        <Form css={tw`m-0`}>
                            <h2 css={tw`text-xl font-header mb-4 text-neutral-200`}>Create Directory</h2>
                            <Field autoFocus id={'directoryName'} name={'directoryName'} label={'Directory Name'} />
                            <p css={tw`mt-2 text-xs text-neutral-400`}>
                                This directory will be created as&nbsp;
                                <Code css={tw`bg-neutral-900 px-1 py-0.5 rounded font-mono text-[11px]`}>
                                    /home/template/
                                    <span css={tw`text-cyan-200`}>
                                        {joinPaths(directory, values.directoryName).replace(/^(\.\.\/|\/)+/, '')}
                                    </span>
                                </Code>
                            </p>
                            <div css={tw`flex justify-end space-x-2 mt-6`}>
                                <Button.Text type={'button'} onClick={() => setFolderModalVisible(false)}>Cancel</Button.Text>
                                <Button type={'button'} onClick={submitForm} disabled={isSubmitting}>Create</Button>
                            </div>
                        </Form>
                    )}
                </Formik>
            </Modal>

            {/* Rename Modal */}
            <Modal visible={renameModalVisible} onDismissed={() => setRenameModalVisible(false)}>
                <Formik
                    onSubmit={handleRename}
                    initialValues={{ name: renameTarget?.name || '' }}
                    enableReinitialize
                    validationSchema={object().shape({
                        name: string().required('A valid name must be provided.'),
                    })}
                >
                    {({ isSubmitting, submitForm }) => (
                        <Form css={tw`m-0`}>
                            <h2 css={tw`text-xl font-header mb-4 text-neutral-200`}>Rename File / Folder</h2>
                            <Field autoFocus id={'name'} name={'name'} label={'File Name'} />
                            <div css={tw`flex justify-end space-x-2 mt-6`}>
                                <Button.Text type={'button'} onClick={() => setRenameModalVisible(false)}>Cancel</Button.Text>
                                <Button type={'button'} onClick={submitForm} disabled={isSubmitting}>Rename</Button>
                            </div>
                        </Form>
                    )}
                </Formik>
            </Modal>

            {/* Move Modal */}
            <Modal visible={moveModalVisible} onDismissed={() => setMoveModalVisible(false)}>
                <Formik
                    onSubmit={handleMove}
                    initialValues={{ name: directory }}
                    enableReinitialize
                    validationSchema={object().shape({
                        name: string().required('A valid destination must be provided.'),
                    })}
                >
                    {({ isSubmitting, submitForm, values }) => (
                        <Form css={tw`m-0`}>
                            <h2 css={tw`text-xl font-header mb-4 text-neutral-200`}>Move File / Folder</h2>
                            <Field autoFocus id={'name'} name={'name'} label={'Target Directory'} description={'Specify target directory path relative to template root.'} />
                            {moveTarget && (
                                <p css={tw`text-xs mt-2 text-neutral-400`}>
                                    <strong css={tw`text-neutral-200`}>New location:</strong>
                                    &nbsp;/home/template/{joinPaths(values.name, moveTarget.name).replace(/^(\.\.\/|\/)+/, '')}
                                </p>
                            )}
                            <div css={tw`flex justify-end space-x-2 mt-6`}>
                                <Button.Text type={'button'} onClick={() => setMoveModalVisible(false)}>Cancel</Button.Text>
                                <Button type={'button'} onClick={submitForm} disabled={isSubmitting}>Move</Button>
                            </div>
                        </Form>
                    )}
                </Formik>
            </Modal>

            <ErrorBoundary>
                <div className={'flex flex-wrap-reverse md:flex-nowrap mb-4 items-center justify-between'}>
                    <div css={tw`flex flex-grow-0 items-center text-sm text-neutral-500 overflow-x-hidden py-2`}>
                        <FileActionCheckbox
                            type={'checkbox'}
                            css={tw`mx-4`}
                            checked={sortedFilesList.length > 0 && selectedFiles.length === sortedFilesList.length}
                            onChange={(e) => {
                                setSelectedFiles(e.currentTarget.checked ? sortedFilesList.map(f => f.name) : []);
                            }}
                        />
                        /<span css={tw`px-1 text-neutral-300`}>home</span>/
                        <Link to={`/stratus/templates/${id}`} css={tw`px-1 text-neutral-200 no-underline hover:text-neutral-100`}>
                            template
                        </Link>
                        /
                        {breadcrumbs().map((crumb, index) =>
                            crumb.path ? (
                                <React.Fragment key={index}>
                                    <a
                                        href={`#${crumb.path}`}
                                        css={tw`px-1 text-neutral-200 no-underline hover:text-neutral-100`}
                                    >
                                        {crumb.name}
                                    </a>
                                    /
                                </React.Fragment>
                            ) : (
                                <span key={index} css={tw`px-1 text-neutral-300`}>
                                    {crumb.name}
                                </span>
                            )
                        )}
                    </div>

                    <div className={styles.manager_actions}>
                        {/* Hidden file input */}
                        <input
                            type={'file'}
                            ref={fileInputRef}
                            onChange={handleUploadChange}
                            className={'hidden'}
                            multiple
                        />
                        
                        <Button.Text onClick={() => setFolderModalVisible(true)}>
                            Create Directory
                        </Button.Text>

                        <Button onClick={handleUploadClick}>
                            Upload
                        </Button>

                        <Link to={`/stratus/templates/${id}/files/new#${directory}`}>
                            <Button>
                                New File
                            </Button>
                        </Link>
                    </div>
                </div>
            </ErrorBoundary>

            {!files ? (
                <Spinner size={'large'} centered />
            ) : (
                <CSSTransition classNames={'fade'} timeout={150} appear in>
                    <div css={tw`flex flex-col`}>
                        {sortedFilesList.length === 0 && (
                            <p css={tw`text-sm text-neutral-400 text-center py-8`}>This directory seems to be empty.</p>
                        )}
                        {sortedFilesList.map(file => {
                            const isArchive = file.name.endsWith('.zip') || file.name.endsWith('.tar.gz');
                            return (
                                <div
                                    key={file.name}
                                    className={styles.file_row}
                                    onContextMenu={(e) => {
                                        e.preventDefault();
                                        const ref = dropdownRefs.current[file.name];
                                        if (ref) {
                                            ref.triggerMenu(e.clientX);
                                        }
                                    }}
                                >
                                    <label css={tw`flex-none px-4 py-2 absolute self-center z-30 cursor-pointer`}>
                                        <FileActionCheckbox
                                            type={'checkbox'}
                                            checked={selectedFiles.includes(file.name)}
                                            onChange={(e) => {
                                                if (e.currentTarget.checked) {
                                                    setSelectedFiles(prev => [...prev, file.name]);
                                                } else {
                                                    setSelectedFiles(prev => prev.filter(n => n !== file.name));
                                                }
                                            }}
                                        />
                                    </label>
                                    {file.isFile ? (
                                        file.isEditable() ? (
                                            <Link 
                                                to={`/stratus/templates/${id}/files/edit#${joinPaths(directory, file.name)}`}
                                                className={styles.details}
                                            >
                                                <div css={tw`flex-none text-neutral-400 ml-6 mr-4 text-lg pl-3`}>
                                                    <FontAwesomeIcon
                                                        icon={isArchive ? faFileArchive : faFileAlt}
                                                    />
                                                </div>
                                                <div css={tw`flex-1 truncate`}>{file.name}</div>
                                                <div css={tw`w-1/6 text-right mr-4 hidden sm:block`}>{bytesToString(file.size)}</div>
                                                <div css={tw`w-1/5 text-right mr-4 hidden md:block`} title={file.modifiedAt.toString()}>
                                                    {Math.abs(differenceInHours(file.modifiedAt, new Date())) > 48
                                                        ? format(file.modifiedAt, 'MMM do, yyyy h:mma')
                                                        : formatDistanceToNow(file.modifiedAt, { addSuffix: true })}
                                                </div>
                                            </Link>
                                        ) : (
                                            <div className={styles.details}>
                                                <div css={tw`flex-none text-neutral-400 ml-6 mr-4 text-lg pl-3`}>
                                                    <FontAwesomeIcon
                                                        icon={isArchive ? faFileArchive : faFileAlt}
                                                    />
                                                </div>
                                                <div css={tw`flex-1 truncate text-neutral-500`}>{file.name}</div>
                                                <div css={tw`w-1/6 text-right mr-4 hidden sm:block text-neutral-500`}>{bytesToString(file.size)}</div>
                                                <div css={tw`w-1/5 text-right mr-4 hidden md:block text-neutral-500`} title={file.modifiedAt.toString()}>
                                                    {Math.abs(differenceInHours(file.modifiedAt, new Date())) > 48
                                                        ? format(file.modifiedAt, 'MMM do, yyyy h:mma')
                                                        : formatDistanceToNow(file.modifiedAt, { addSuffix: true })}
                                                </div>
                                            </div>
                                        )
                                    ) : (
                                        <a 
                                            href={`#${joinPaths(directory, file.name)}`} 
                                            className={styles.details}
                                        >
                                            <div css={tw`flex-none text-neutral-400 ml-6 mr-4 text-lg pl-3`}>
                                                <FontAwesomeIcon icon={faFolder} />
                                            </div>
                                            <div css={tw`flex-1 truncate`}>{file.name}</div>
                                            <div css={tw`w-1/6 text-right mr-4 hidden sm:block`}>--</div>
                                            <div css={tw`w-1/5 text-right mr-4 hidden md:block`} title={file.modifiedAt.toString()}>
                                                {Math.abs(differenceInHours(file.modifiedAt, new Date())) > 48
                                                    ? format(file.modifiedAt, 'MMM do, yyyy h:mma')
                                                    : formatDistanceToNow(file.modifiedAt, { addSuffix: true })}
                                            </div>
                                        </a>
                                    )}
                                    <div css={tw`flex items-center z-30`}>
                                        <DropdownMenu
                                            ref={(ref) => {
                                                dropdownRefs.current[file.name] = ref;
                                            }}
                                            renderToggle={(onClick) => (
                                                <div 
                                                    onClick={onClick}
                                                    css={tw`px-4 py-2 hover:text-white transition-colors cursor-pointer`}
                                                >
                                                    <FontAwesomeIcon icon={faEllipsisH} />
                                                </div>
                                            )}
                                        >
                                            <div 
                                                onClick={() => {
                                                    setRenameTarget(file);
                                                    setRenameModalVisible(true);
                                                }} 
                                                css={tw`p-2 flex items-center rounded hover:bg-neutral-100 hover:text-neutral-700 cursor-pointer text-xs text-neutral-300`}
                                            >
                                                <FontAwesomeIcon icon={faPencilAlt} css={tw`text-xs`} fixedWidth />
                                                <span css={tw`ml-2`}>Rename</span>
                                            </div>
                                            <div 
                                                onClick={() => {
                                                    setMoveTarget(file);
                                                    setMoveModalVisible(true);
                                                }} 
                                                css={tw`p-2 flex items-center rounded hover:bg-neutral-100 hover:text-neutral-700 cursor-pointer text-xs text-neutral-300`}
                                            >
                                                <FontAwesomeIcon icon={faLevelUpAlt} css={tw`text-xs`} fixedWidth />
                                                <span css={tw`ml-2`}>Move</span>
                                            </div>
                                            {isArchive ? (
                                                <div 
                                                    onClick={() => handleUnarchive(file.name)} 
                                                    css={tw`p-2 flex items-center rounded hover:bg-neutral-100 hover:text-neutral-700 cursor-pointer text-xs text-neutral-300`}
                                                >
                                                    <FontAwesomeIcon icon={faBoxOpen} css={tw`text-xs`} fixedWidth />
                                                    <span css={tw`ml-2`}>Unarchive</span>
                                                </div>
                                            ) : (
                                                <div 
                                                    onClick={() => handleArchive(file.name)} 
                                                    css={tw`p-2 flex items-center rounded hover:bg-neutral-100 hover:text-neutral-700 cursor-pointer text-xs text-neutral-300`}
                                                >
                                                    <FontAwesomeIcon icon={faFileArchive} css={tw`text-xs`} fixedWidth />
                                                    <span css={tw`ml-2`}>Archive</span>
                                                </div>
                                            )}
                                            {file.isFile && (
                                                <div 
                                                    onClick={() => handleDownload(file.name)} 
                                                    css={tw`p-2 flex items-center rounded hover:bg-neutral-100 hover:text-neutral-700 cursor-pointer text-xs text-neutral-300`}
                                                >
                                                    <FontAwesomeIcon icon={faFileDownload} css={tw`text-xs`} fixedWidth />
                                                    <span css={tw`ml-2`}>Download</span>
                                                </div>
                                            )}
                                            <div 
                                                onClick={() => setFileToDelete(file.name)} 
                                                css={tw`p-2 flex items-center rounded hover:bg-red-100 hover:text-red-700 cursor-pointer text-xs text-red-400 border-t border-neutral-800/50`}
                                            >
                                                <FontAwesomeIcon icon={faTrashAlt} css={tw`text-xs`} fixedWidth />
                                                <span css={tw`ml-2`}>Delete</span>
                                            </div>
                                        </DropdownMenu>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </CSSTransition>
            )}

            {/* Mass Actions Bar Portal */}
            <Portal>
                <div className={'pointer-events-none fixed bottom-0 mb-6 flex justify-center w-full z-50'} style={{ left: 0 }}>
                    <Fade timeout={75} in={selectedFiles.length > 0} unmountOnExit>
                        <div css={tw`flex items-center space-x-4 pointer-events-auto rounded p-4 bg-black/80 border border-neutral-700 shadow-2xl`}>
                            <span css={tw`text-sm text-neutral-300 mr-2`}>
                                {selectedFiles.length} item{selectedFiles.length > 1 ? 's' : ''} selected
                            </span>
                            <Button color={'grey'} onClick={() => setSelectedFiles([])} size={'small'}>
                                Deselect
                            </Button>
                            <Button color={'grey'} onClick={handleMassCompress} size={'small'}>
                                Archive
                            </Button>
                            <Button color={'red'} onClick={() => setShowMassDeleteConfirm(true)} size={'small'}>
                                Delete
                            </Button>
                        </div>
                    </Fade>
                </div>
            </Portal>
        </div>
    );
};
