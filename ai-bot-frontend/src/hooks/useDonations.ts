import { useCallback, useEffect, useState } from 'react';
import type { CreateDonationBatchRequest, DonationBatchResponse } from '../api/types/donation.ts';
import donationApi from '../api/donationApi.ts';
import useSnackbar from './useSnackbar.ts';

const useDonations = () => {
  const { showSnackbar } = useSnackbar();

  const [donations, setDonations] = useState<DonationBatchResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const fetch = useCallback(async () => {
    setLoading(true);
    try {
      const response = await donationApi.findAll();
      setDonations(response.data);
    } catch (err) {
      showSnackbar(err instanceof Error ? err.message : 'Failed to load donation batches.', 'error');
    } finally {
      setLoading(false);
    }
  }, [showSnackbar]);

  useEffect(() => { void fetch(); }, [fetch]);

  const onCreate = async (data: CreateDonationBatchRequest) => {
    try {
      await donationApi.add(data);
      await fetch();
    } catch (err) {
      showSnackbar(err instanceof Error ? err.message : 'Failed to create donation batch.', 'error');
    }
  };

  const onApprove = async (id: number) => {
    try {
      await donationApi.approve(id.toString());
      await fetch();
    } catch (err) {
      showSnackbar(err instanceof Error ? err.message : 'Failed to approve donation batch.', 'error');
    }
  };

  const onSubmit = async (id: number) => {
    try {
      await donationApi.submit(id.toString());
      await fetch();
    } catch (err) {
      showSnackbar(err instanceof Error ? err.message : 'Failed to submit donation batch.', 'error');
    }
  };

  return { donations, loading, onCreate, onApprove, onSubmit };
};

export default useDonations;