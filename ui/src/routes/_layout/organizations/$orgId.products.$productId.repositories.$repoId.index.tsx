/*
 * Copyright (C) 2024 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

import { useSuspenseQueries } from '@tanstack/react-query';
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router';
import { EditIcon, PlusIcon, TrashIcon } from 'lucide-react';

import {
  useRepositoriesServiceDeleteRepositoryById,
  useRepositoriesServiceGetOrtRunsKey,
  useRepositoriesServiceGetRepositoryByIdKey,
} from '@/api/queries';
import { ApiError, RepositoriesService } from '@/api/requests';
import { OrtRunJobStatus } from '@/components/ort-run-job-status';
import { ToastError } from '@/components/toast-error';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { useToast } from '@/components/ui/use-toast';
import { calculateDuration } from '@/helpers/get-run-duration';
import { getStatusBackgroundColor } from '@/helpers/get-status-colors';

const pollInterval =
  Number.parseInt(import.meta.env.VITE_RUN_POLL_INTERVAL) || 10000;

const RepoComponent = () => {
  const params = Route.useParams();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [{ data: repo }, { data: runs }] = useSuspenseQueries({
    queries: [
      {
        queryKey: [useRepositoriesServiceGetRepositoryByIdKey, params.repoId],
        queryFn: async () =>
          await RepositoriesService.getRepositoryById({
            repositoryId: Number.parseInt(params.repoId),
          }),
      },
      {
        queryKey: [useRepositoriesServiceGetOrtRunsKey, params.repoId],
        queryFn: async () =>
          await RepositoriesService.getOrtRuns({
            repositoryId: Number.parseInt(params.repoId),
            sort: '-index',
          }),
        refetchInterval: pollInterval,
      },
    ],
  });

  const { mutateAsync: deleteRepository } =
    useRepositoriesServiceDeleteRepositoryById({
      onSuccess() {
        toast({
          title: 'Delete Repository',
          description: 'Repository deleted successfully.',
        });
        navigate({
          to: '/organizations/$orgId/products/$productId',
          params: { orgId: params.orgId, productId: params.productId },
        });
      },
      onError(error: ApiError) {
        toast({
          title: error.message,
          description: <ToastError error={error} />,
          variant: 'destructive',
        });
      },
    });

  async function handleDelete() {
    await deleteRepository({
      repositoryId: Number.parseInt(params.repoId),
    });
  }

  return (
    <TooltipProvider>
      <Card className='mx-auto w-full max-w-4xl'>
        <CardHeader>
          <CardTitle className='flex flex-row justify-between'>
            <div className='flex items-stretch'>
              <div className='flex items-center pb-1'>{repo.url}</div>
              <Tooltip>
                <TooltipTrigger>
                  <Button
                    asChild
                    size='sm'
                    variant='outline'
                    className='ml-2 px-2'
                  >
                    <Link
                      to='/organizations/$orgId/products/$productId/repositories/$repoId/edit'
                      params={{
                        orgId: params.orgId,
                        productId: params.productId,
                        repoId: params.repoId,
                      }}
                    >
                      <EditIcon className='h-4 w-4' />
                    </Link>
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Edit this repository</TooltipContent>
              </Tooltip>
            </div>
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button
                  size='sm'
                  variant='destructive'
                  className='px-2 hover:bg-red-700'
                >
                  <TrashIcon className='h-4 w-4' />
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Delete repository</AlertDialogTitle>
                </AlertDialogHeader>
                <AlertDialogDescription>
                  Are you sure you want to delete this repository?
                </AlertDialogDescription>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction onClick={handleDelete}>
                    Delete
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </CardTitle>
          <CardDescription>{repo.type}</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Run</TableHead>
                <TableHead>Created At</TableHead>
                <TableHead>Run Status</TableHead>
                <TableHead>Job Statuses</TableHead>
                <TableHead>Duration</TableHead>
                <TableHead className='pb-1.5 pr-0 text-right'>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button asChild size='sm' className='ml-auto gap-1'>
                        <Link
                          to='/organizations/$orgId/products/$productId/repositories/$repoId/create-run'
                          params={{
                            orgId: params.orgId,
                            productId: params.productId,
                            repoId: params.repoId,
                          }}
                        >
                          New run
                          <PlusIcon className='h-4 w-4' />
                        </Link>
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>
                      Create a new ORT run for this repository
                    </TooltipContent>
                  </Tooltip>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {runs?.data.map((run) => {
                return (
                  <TableRow key={run.id}>
                    <TableCell>
                      <div>
                        <Link
                          to={
                            '/organizations/$orgId/products/$productId/repositories/$repoId/runs/$runId'
                          }
                          params={{
                            orgId: params.orgId,
                            productId: params.productId,
                            repoId: repo.id.toString(),
                            runId: run.id.toString(),
                          }}
                          className='font-semibold text-blue-400 hover:underline'
                        >
                          {run.index}
                        </Link>
                      </div>
                    </TableCell>
                    <TableCell>
                      {new Date(run.createdAt).toLocaleString().split(',')[0]}
                    </TableCell>
                    <TableCell>
                      <Badge
                        className={`border ${getStatusBackgroundColor(run.status)}`}
                      >
                        {run.status}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <OrtRunJobStatus jobs={run.jobs} />
                    </TableCell>
                    <TableCell>
                      {run.finishedAt
                        ? calculateDuration(run.createdAt, run.finishedAt)
                        : '-'}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </TooltipProvider>
  );
};

export const Route = createFileRoute(
  '/_layout/organizations/$orgId/products/$productId/repositories/$repoId/'
)({
  loader: async ({ context, params }) => {
    await Promise.allSettled([
      context.queryClient.ensureQueryData({
        queryKey: [useRepositoriesServiceGetRepositoryByIdKey, params.repoId],
        queryFn: () =>
          RepositoriesService.getRepositoryById({
            repositoryId: Number.parseInt(params.repoId),
          }),
      }),
      context.queryClient.ensureQueryData({
        queryKey: [useRepositoriesServiceGetOrtRunsKey, params.repoId],
        queryFn: () =>
          RepositoriesService.getOrtRuns({
            repositoryId: Number.parseInt(params.repoId),
            sort: '-index',
          }),
      }),
    ]);
  },
  component: RepoComponent,
});
