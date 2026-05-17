import React, { useEffect, useState, useRef } from 'react';
import { useLocation, useParams, Link } from 'react-router-dom';
import useSWR from 'swr';
import getTemplateFiles from '@/api/stratus/templates/getTemplateFiles';
import PageContentBlock from '@/components/elements/PageContentBlock';
import Spinner from '@/components/elements/Spinner';
import tw from 'twin.macro';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFolder, faFileAlt, faLevelUpAlt, faTrashAlt, faUpload, faFolderPlus, faFileMedical } from '@fortawesome/free-solid-svg-icons';
import { bytesToString } from '@/lib/formatters';
import { format } from 'date-fns';
import http from '@/api/http';
import Modal from '@/components/elements/Modal';
import Button from '@/components/elements/Button';
import SpinnerOverlay from '@/components/elements/SpinnerOverlay';

const joinPaths = (a: string, b: string) => (a === '/' ? '/' + b : a + '/' + b).replace(/\/+/g, '/');

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

    const handleDelete = (fileName: string) => {
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

    if (error) return <PageContentBlock title={'Error'}><div>Failed to load template files.</div></PageContentBlock>;

    return (
        <PageContentBlock title={'Template File Manager'}>
            <div className={'bg-neutral-900 rounded shadow-lg overflow-hidden relative'}>
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

                <div className={'p-4 bg-neutral-800 border-b border-neutral-700 flex flex-wrap gap-4 justify-between items-center'}>
                    <div className={'flex items-center space-x-2'}>
                        <h2 className={'text-lg font-header text-neutral-200'}>/ {directory.replace(/^\//, '')}</h2>
                    </div>

                    <div className={'flex flex-wrap gap-2 items-center'}>
                        {/* Hidden file input */}
                        <input
                            type={'file'}
                            ref={fileInputRef}
                            onChange={handleUploadChange}
                            className={'hidden'}
                        />
                        
                        <label className={'flex items-center space-x-2 text-xs text-neutral-400 bg-neutral-700/50 px-3 py-2 rounded border border-neutral-600 cursor-pointer hover:border-neutral-500 transition-colors mr-2'}>
                            <input
                                type={'checkbox'}
                                checked={extractZip}
                                onChange={(e) => setExtractZip(e.target.checked)}
                                className={'rounded border-neutral-600 bg-neutral-800 text-cyan-600 focus:ring-cyan-500 focus:ring-offset-neutral-900'}
                            />
                            <span>Extract ZIPs</span>
                        </label>

                        <Button size={'small'} color={'cyan'} onClick={handleUploadClick} className={'flex items-center space-x-2'}>
                            <FontAwesomeIcon icon={faUpload} />
                            <span>Upload File</span>
                        </Button>

                        <Button size={'small'} color={'grey'} onClick={() => setFolderModalVisible(true)} className={'flex items-center space-x-2'}>
                            <FontAwesomeIcon icon={faFolderPlus} />
                            <span>New Folder</span>
                        </Button>

                        <Link to={`/stratus/templates/${id}/files/new#${directory}`}>
                            <Button size={'small'} className={'flex items-center space-x-2'}>
                                <FontAwesomeIcon icon={faFileMedical} />
                                <span>New File</span>
                            </Button>
                        </Link>
                    </div>
                </div>
                
                {!files ? (
                    <div className={'p-20'}><Spinner centered /></div>
                ) : (
                    <div className={'flex flex-col'}>
                        {directory !== '/' && (
                            <a 
                                href={`#${directory.split('/').slice(0, -1).join('/') || '/'}`}
                                className={'flex items-center p-3 border-b border-neutral-800 hover:bg-neutral-800 transition-colors text-cyan-400 font-medium'}
                            >
                                <FontAwesomeIcon icon={faLevelUpAlt} className={'mr-3'} />
                                <span>Go Back</span>
                            </a>
                        )}
                        {files.length === 0 && (
                            <div className={'p-10 text-center text-neutral-500 italic'}>
                                This directory is empty.
                            </div>
                        )}
                        {files.map(file => (
                            <div key={file.name} className={'flex items-center p-3 border-b border-neutral-800 hover:bg-neutral-800 transition-colors group'}>
                                <div className={'w-10 text-center text-neutral-400'}>
                                    <FontAwesomeIcon icon={file.isFile ? faFileAlt : faFolder} />
                                </div>
                                <div className={'flex-1 truncate'}>
                                    {file.isFile ? (
                                        file.isEditable() ? (
                                            <Link 
                                                to={`/stratus/templates/${id}/files/edit#${joinPaths(directory, file.name)}`}
                                                className={'text-neutral-200 hover:text-cyan-400 transition-colors hover:underline'}
                                            >
                                                {file.name}
                                            </Link>
                                        ) : (
                                            <span className={'text-neutral-400'}>{file.name}</span>
                                        )
                                    ) : (
                                        <a href={`#${joinPaths(directory, file.name)}`} className={'text-cyan-400 hover:underline font-medium'}>
                                            {file.name}
                                        </a>
                                    )}
                                </div>
                                <div className={'w-24 text-right text-xs text-neutral-500 mr-4'}>
                                    {file.isFile ? bytesToString(file.size) : '--'}
                                </div>
                                <div className={'w-40 text-right text-xs text-neutral-500 hidden md:block mr-4'}>
                                    {format(file.modifiedAt, 'MMM do, yyyy HH:mm')}
                                </div>
                                <div className={'w-12 text-center'}>
                                    {file.name !== '.keep' && (
                                        <button 
                                            onClick={() => handleDelete(file.name)}
                                            className={'text-neutral-500 hover:text-red-500 p-2 transition-colors opacity-0 group-hover:opacity-100 focus:opacity-100'}
                                            title={'Delete'}
                                        >
                                            <FontAwesomeIcon icon={faTrashAlt} />
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </PageContentBlock>
    );
};
