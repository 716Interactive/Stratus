import React, { useEffect, useState, useRef } from 'react';
import { useLocation, useParams, Link } from 'react-router-dom';
import useSWR from 'swr';
import getTemplateFiles from '@/api/stratus/templates/getTemplateFiles';
import PageContentBlock from '@/components/elements/PageContentBlock';
import Spinner from '@/components/elements/Spinner';
import tw from 'twin.macro';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFolder, faFileAlt, faFileArchive, faTrashAlt, faUpload, faFolderPlus, faFileMedical } from '@fortawesome/free-solid-svg-icons';
import { bytesToString } from '@/lib/formatters';
import { differenceInHours, format, formatDistanceToNow } from 'date-fns';
import http from '@/api/http';
import Modal from '@/components/elements/Modal';
import Button from '@/components/elements/Button';
import SpinnerOverlay from '@/components/elements/SpinnerOverlay';
import ErrorBoundary from '@/components/elements/ErrorBoundary';
import { FileObject } from '@/api/server/files/loadDirectory';
import styles from '@/components/server/files/style.module.css';

const joinPaths = (a: string, b: string) => (a === '/' ? '/' + b : a + '/' + b).replace(/\/+/g, '/');

const sortFiles = (files: FileObject[]): FileObject[] => {
    const sortedFiles: FileObject[] = files
        .sort((a, b) => a.name.localeCompare(b.name))
        .sort((a, b) => (a.isFile === b.isFile ? 0 : a.isFile ? 1 : -1));
    return sortedFiles;
};

export default () => {
    const { id } = useParams<{ id: string }>();
    const { hash } = useLocation();
    const [directory, setDirectory] = useState(hash.replace('#', '') || '/');
    const [loading, setLoading] = useState(false);
    
    // Create folder modal state
    const [folderModalVisible, setFolderModalVisible] = useState(false);
    const [folderName, setFolderName] = useState('');

    const fileInputRef = useRef<HTMLInputElement>(null);
    const [extractZip, setExtractZip] = useState(true);

    const { data: files, error, mutate } = useSWR(
        [`/stratus/templates/${id}/files/list`, directory],
        () => getTemplateFiles(id, directory)
    );

    useEffect(() => {
        setDirectory(hash.replace('#', '') || '/');
    }, [hash]);

    const handleUploadClick = () => {
        fileInputRef.current?.click();
    };

    const handleUploadChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (!e.target.files || e.target.files.length === 0) return;
        
        setLoading(true);
        const file = e.target.files[0];
        const formData = new FormData();
        formData.append('file', file);
        formData.append('directory', directory);
        formData.append('extract', extractZip ? 'true' : 'false');

        http.post(`/api/client/stratus/templates/${id}/files/upload`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
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

    const handleCreateFolder = (e: React.FormEvent) => {
        e.preventDefault();
        if (!folderName.trim()) return;

        setLoading(true);
        const targetPath = joinPaths(directory, `${folderName.trim()}/.keep`);

        http.post(`/api/client/stratus/templates/${id}/files/write`, '', {
            params: { file: targetPath },
            headers: { 'Content-Type': 'text/plain' }
        })
        .then(() => {
            setFolderName('');
            setFolderModalVisible(false);
            mutate();
        })
        .catch(err => {
            console.error(err);
            alert('Failed to create folder: ' + err.message);
        })
        .then(() => setLoading(false));
    };

    const handleDelete = (fileName: string, e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();
        if (!confirm(`Are you sure you want to delete ${fileName}?`)) return;

        setLoading(true);
        const targetPath = joinPaths(directory, fileName);

        http.post(`/api/client/stratus/templates/${id}/files/delete`, { files: [targetPath] })
        .then(() => mutate())
        .catch(err => {
            console.error(err);
            alert('Delete failed: ' + err.message);
        })
        .then(() => setLoading(false));
    };

    const breadcrumbs = (): { name: string; path?: string }[] =>
        directory
            .split('/')
            .filter((dir) => !!dir)
            .map((dir, index, dirs) => {
                const path = `/${dirs.slice(0, index + 1).join('/')}`;
                return { name: dir, path };
            });

    if (error) return <PageContentBlock title={'Error'}><div>Failed to load template files.</div></PageContentBlock>;

    return (
        <PageContentBlock title={'Template File Manager'}>
            <div className={'relative'}>
                <SpinnerOverlay visible={loading} />

                {/* Create Folder Modal */}
                <Modal visible={folderModalVisible} onDismissed={() => setFolderModalVisible(false)}>
                    <form onSubmit={handleCreateFolder}>
                        <h2 className={'text-xl font-header mb-4 text-neutral-200'}>Create Folder</h2>
                        <input
                            type={'text'}
                            value={folderName}
                            onChange={(e) => setFolderName(e.target.value)}
                            placeholder={'Folder Name'}
                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500 mb-4'}
                            autoFocus
                        />
                        <div className={'flex justify-end space-x-2'}>
                            <Button type={'button'} color={'grey'} onClick={() => setFolderModalVisible(false)}>Cancel</Button>
                            <Button type={'submit'}>Create</Button>
                        </div>
                    </form>
                </Modal>

                <ErrorBoundary>
                    <div className={'flex flex-wrap-reverse md:flex-nowrap mb-4 items-center justify-between'}>
                        <div css={tw`flex flex-grow-0 items-center text-sm text-neutral-500 overflow-x-hidden py-2`}>
                            <div css={tw`w-8`} />/<span css={tw`px-1 text-neutral-300`}>home</span>/
                            <Link to={`/stratus/templates/${id}/files`} css={tw`px-1 text-neutral-200 no-underline hover:text-neutral-100`}>
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
                            />
                            
                            <label className={'flex items-center space-x-2 text-xs text-neutral-400 bg-neutral-700/50 px-3 py-2 rounded border border-neutral-600 cursor-pointer hover:border-neutral-500 transition-colors justify-center md:mr-2'}>
                                <input
                                    type={'checkbox'}
                                    checked={extractZip}
                                    onChange={(e) => setExtractZip(e.target.checked)}
                                    className={'rounded border-neutral-600 bg-neutral-800 text-cyan-600 focus:ring-cyan-500 focus:ring-offset-neutral-900'}
                                />
                                <span>Extract ZIPs</span>
                            </label>

                            <Button onClick={() => setFolderModalVisible(true)} className={'flex items-center space-x-2 justify-center'}>
                                <FontAwesomeIcon icon={faFolderPlus} />
                                <span>New Folder</span>
                            </Button>

                            <Button onClick={handleUploadClick} className={'flex items-center space-x-2 justify-center'}>
                                <FontAwesomeIcon icon={faUpload} />
                                <span>Upload</span>
                            </Button>

                            <Link to={`/stratus/templates/${id}/files/new#${directory}`} css={tw`w-full`}>
                                <Button css={tw`w-full flex items-center space-x-2 justify-center`}>
                                    <FontAwesomeIcon icon={faFileMedical} />
                                    <span>New File</span>
                                </Button>
                            </Link>
                        </div>
                    </div>
                </ErrorBoundary>

                {!files ? (
                    <Spinner size={'large'} centered />
                ) : (
                    <div css={tw`flex flex-col`}>
                        {directory !== '/' && (
                            <a 
                                href={`#${directory.split('/').slice(0, -1).join('/') || '/'}`}
                                className={styles.file_row}
                            >
                                <div className={styles.details}>
                                    <div css={tw`flex-none text-neutral-400 ml-6 mr-4 text-lg pl-3`}>
                                        <FontAwesomeIcon icon={faFolder} />
                                    </div>
                                    <span css={tw`text-cyan-400 font-medium`}>..</span>
                                </div>
                            </a>
                        )}
                        {files.length === 0 && (
                            <p css={tw`text-sm text-neutral-400 text-center py-8`}>This directory seems to be empty.</p>
                        )}
                        {sortFiles(files).map(file => (
                            <div
                                key={file.name}
                                className={styles.file_row}
                            >
                                {file.isFile ? (
                                    file.isEditable() ? (
                                        <Link 
                                            to={`/stratus/templates/${id}/files/edit#${joinPaths(directory, file.name)}`}
                                            className={styles.details}
                                        >
                                            <div css={tw`flex-none text-neutral-400 ml-6 mr-4 text-lg pl-3`}>
                                                <FontAwesomeIcon
                                                    icon={file.name.endsWith('.zip') || file.name.endsWith('.tar.gz') ? faFileArchive : faFileAlt}
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
                                                    icon={file.name.endsWith('.zip') || file.name.endsWith('.tar.gz') ? faFileArchive : faFileAlt}
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
                                        <div css={tw`flex-1 truncate text-cyan-400 font-medium`}>{file.name}</div>
                                        <div css={tw`w-1/6 text-right mr-4 hidden sm:block`}>--</div>
                                        <div css={tw`w-1/5 text-right mr-4 hidden md:block`} title={file.modifiedAt.toString()}>
                                            {Math.abs(differenceInHours(file.modifiedAt, new Date())) > 48
                                                ? format(file.modifiedAt, 'MMM do, yyyy h:mma')
                                                : formatDistanceToNow(file.modifiedAt, { addSuffix: true })}
                                        </div>
                                    </a>
                                )}
                                {file.name !== '.keep' && (
                                    <button 
                                        onClick={(e) => handleDelete(file.name, e)}
                                        css={tw`text-neutral-500 hover:text-red-500 p-2 mr-4 transition-colors focus:outline-none`}
                                        title={'Delete'}
                                    >
                                        <FontAwesomeIcon icon={faTrashAlt} />
                                    </button>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </PageContentBlock>
    );
};
