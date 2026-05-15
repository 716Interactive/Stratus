import React, { useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import useSWR from 'swr';
import getTemplateFiles from '@/api/stratus/templates/getTemplateFiles';
import PageContentBlock from '@/components/elements/PageContentBlock';
import Spinner from '@/components/elements/Spinner';
import tw from 'twin.macro';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFolder, faFileAlt, faLevelUpAlt } from '@fortawesome/free-solid-svg-icons';
import { bytesToString } from '@/lib/formatters';
import { format } from 'date-fns';

export default () => {
    const { id } = useParams<{ id: string }>();
    const { hash } = useLocation();
    const [directory, setDirectory] = useState(hash.replace('#', '') || '/');

    const { data: files, error, mutate } = useSWR(
        [`/stratus/templates/${id}/files/list`, directory],
        () => getTemplateFiles(id, directory)
    );

    useEffect(() => {
        setDirectory(hash.replace('#', '') || '/');
    }, [hash]);

    if (error) return <PageContentBlock title={'Error'}><div>Failed to load template files.</div></PageContentBlock>;

    return (
        <PageContentBlock title={'Template File Manager'}>
            <div className={'bg-neutral-900 rounded shadow-lg overflow-hidden'}>
                <div className={'p-4 bg-neutral-800 border-b border-neutral-700 flex justify-between items-center'}>
                    <div className={'flex items-center space-x-2'}>
                        <h2 className={'text-lg font-header'}>/ {directory.replace(/^\//, '')}</h2>
                    </div>
                </div>
                
                {!files ? (
                    <div className={'p-20'}><Spinner centered /></div>
                ) : (
                    <div className={'flex flex-col'}>
                        {directory !== '/' && (
                            <a 
                                href={`#${directory.split('/').slice(0, -1).join('/') || '/'}`}
                                className={'flex items-center p-3 border-b border-neutral-800 hover:bg-neutral-800 transition-colors text-cyan-400'}
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
                                        <span className={'text-neutral-200'}>{file.name}</span>
                                    ) : (
                                        <a href={`#${(directory === '/' ? '' : directory) + '/' + file.name}`} className={'text-cyan-400 hover:underline'}>
                                            {file.name}
                                        </a>
                                    )}
                                </div>
                                <div className={'w-24 text-right text-xs text-neutral-500 mr-4'}>
                                    {file.isFile ? bytesToString(file.size) : '--'}
                                </div>
                                <div className={'w-40 text-right text-xs text-neutral-500 hidden md:block'}>
                                    {format(file.modifiedAt, 'MMM do, yyyy HH:mm')}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </PageContentBlock>
    );
};
