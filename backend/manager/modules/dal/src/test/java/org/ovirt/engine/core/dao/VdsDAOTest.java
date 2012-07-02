package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VDSType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
public class VdsDAOTest extends BaseDAOTestCase {
    private static final Guid EXISTING_VDS_ID = new Guid("afce7a39-8e8c-4819-ba9c-796d316592e7");

    private static final String IP_ADDRESS = "192.168.122.17";
    private VdsDAO dao;
    private VDS existingVds;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dao = prepareDAO(dbFacade.getVdsDAO());
        existingVds = dao.get(EXISTING_VDS_ID);
    }

    /**
     * Ensures that retrieving with an invalid ID returns null.
     */
    @Test
    public void testGetWithInvalidId() {
        VDS result = dao.get(NGuid.NewGuid());

        assertNull(result);
    }

    /**
     * Ensures that retrieving VDS by ID works as expected.
     */
    @Test
    public void testGet() {
        VDS result = dao.get(existingVds.getId());

        assertCorrectGetResult(result);
    }

    /**
     * Ensures that retrieving VDS by ID works as expected for a privileged user.
     */
    @Test
    public void testGetWithPermissionsPrivilegedUser() {
        VDS result = dao.get(existingVds.getId(), PRIVILEGED_USER_ID, true);

        assertCorrectGetResult(result);
    }

    /**
     * Ensures that retrieving VDS by ID works as expected with filtering disabled for an unprivileged user.
     */
    @Test
    public void testGetWithPermissionsDisabledUnprivilegedUser() {
        VDS result = dao.get(existingVds.getId(), UNPRIVILEGED_USER_ID, false);

        assertCorrectGetResult(result);
    }

    /**
     * Ensures that no VDS is retrieved for an unprivileged user.
     */
    @Test
    public void testGetWithPermissionsUnprivilegedUser() {
        VDS result = dao.get(existingVds.getId(), UNPRIVILEGED_USER_ID, true);

        assertNull(result);
    }

    /**
     * Ensures that an empty collection is returned.
     */
    @Test
    public void testGetAllWithNameUsingInvalidName() {
        List<VDS> result = dao.getAllWithName("farkle");

        assertIncorrectGetResult(result);
    }

    /**
     * Asserts the result from {@link VdsDAO#get(NGuid)} is correct.
     * @param result
     */
    private void assertCorrectGetResult(VDS result) {
        assertNotNull(result);
        assertEquals(existingVds, result);
    }

    /**
     * Asserts the result from a call to {@link VdsDAO#get(NGuid)}
     * that isn't supposed to return any data is indeed empty.
     * @param result
     */
    private static void assertIncorrectGetResult(List<VDS> result) {
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Ensures the right set of objects are returned with the given name.
     */
    @Test
    public void testGetAllWithName() {
        List<VDS> result = dao.getAllWithName(existingVds.getvds_name());

        assertNotNull(result);
        assertFalse(result.isEmpty());
        for (VDS vds : result) {
            assertEquals(existingVds.getvds_name(), vds.getvds_name());
        }
    }

    /**
     * Ensures that the right set of VDS instances are returned for the given hostname.
     */
    @Test
    public void testGetAllForHostname() {
        List<VDS> result = dao.getAllForHostname(existingVds.gethost_name());

        assertNotNull(result);
        assertFalse(result.isEmpty());
        for (VDS vds : result) {
            assertEquals(existingVds.gethost_name(), vds.gethost_name());
        }
    }

    /**
     * Ensures that the right set of VDS instances are returned.
     */
    @Test
    public void testGetAllWithIpAddress() {
        List<VDS> result = dao.getAllWithIpAddress(IP_ADDRESS);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Ensures the right set of VDS instances are returned.
     */
    @Test
    public void testGetAllWithUniqueId() {
        List<VDS> result = dao.getAllWithUniqueId(existingVds.getUniqueId());

        assertNotNull(result);
        assertFalse(result.isEmpty());
        for (VDS vds : result) {
            assertEquals(existingVds.getUniqueId(), vds.getUniqueId());
        }
    }

    /**
     * Ensures that an empty collection is returned if the type is not present.
     */
    @Test
    public void testGetVdsToRun() {
        List<VDS> result = dao.getVdsToRun(VDSType.VDS, new Guid("b399944a-81ab-4ec5-8266-e19ba7c3c9d1"), VDSStatus.Up, 0);

        assertIncorrectGetResult(result);
    }

    /**
     * Ensures the API works as expected.
     */
    @Test
    public void testGetAllForVdsGroupWithoutMigrating() {
        List<VDS> result = dao.getAllForVdsGroupWithoutMigrating(existingVds
                .getvds_group_id());

        assertNotNull(result);
        assertFalse(result.isEmpty());
        for (VDS vds : result) {
            assertEquals(existingVds.getvds_group_id(), vds.getvds_group_id());
        }
    }

    /**
     * Ensures that all VDS instances are returned.
     */
    @Test
    public void testGetAll() {
        List<VDS> result = dao.getAll();

        assertCorrectGetAllResult(result);
    }

    /**
     * Ensures that retrieving VDS works as expected for a privileged user.
     */
    @Test
    public void testGetAllWithPermissionsPrivilegedUser() {
        List<VDS> result = dao.getAll(PRIVILEGED_USER_ID, true);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains(existingVds));
    }

    /**
     * Ensures that retrieving VDS works as expected with filtering disabled for an unprivileged user.
     */
    @Test
    public void testGetAllWithPermissionsDisabledUnprivilegedUser() {
        List<VDS> result = dao.getAll(UNPRIVILEGED_USER_ID, false);

        assertCorrectGetAllResult(result);
    }

    /**
     * Ensures that no VDS is retrieved for an unprivileged user with filtering enabled.
     */
    @Test
    public void testGetAllWithPermissionsUnprivilegedUser() {
        List<VDS> result = dao.getAll(UNPRIVILEGED_USER_ID, true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Ensures that all VDS related to the VDS group supplied.
     */
    @Test
    public void testGetAllForVdsGroup() {
        List<VDS> result = dao.getAllForVdsGroup(existingVds.getvds_group_id());

        assertNotNull(result);
        assertFalse(result.isEmpty());
        for (VDS vds : result) {
            assertEquals(existingVds.getvds_group_id(), vds.getvds_group_id());
        }
    }

    /**
     * Ensures that the VDS instances are returned according to spm priority
     */
    @Test
    public void testGetListForSpmSelection() {
        final Guid STORAGE_POOL_ID = new Guid("6d849ebf-755f-4552-ad09-9a090cda105d");
        List<VDS> result = dao.getListForSpmSelection(STORAGE_POOL_ID);
        assertTrue(result.get(0).getVdsSpmPriority() >= result.get(1).getVdsSpmPriority());
    }

    /**
     * Asserts that the right collection containing the existing host is returned for a privileged user with filtering enabled
     */
    @Test
    public void testGetAllForVdsGroupWithPermissionsForPriviligedUser() {
        List<VDS> result = dao.getAllForVdsGroup(existingVds.getvds_group_id(), PRIVILEGED_USER_ID, true);
        assertGetAllForVdsGroupCorrectResult(result);
    }

    /**
     * Asserts that an empty collection is returned for an non privileged user with filtering enabled
     */
    @Test
    public void testGetAllForVdsGroupWithPermissionsForUnpriviligedUser() {
        List<VDS> result = dao.getAllForVdsGroup(existingVds.getvds_group_id(), UNPRIVILEGED_USER_ID, true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Asserts that the right collection containing the existing host is returned for a non privileged user with filtering disabled
     */
    @Test
    public void testGetAllForVdsGroupWithPermissionsDisabledForUnpriviligedUser() {
        List<VDS> result = dao.getAllForVdsGroup(existingVds.getvds_group_id(), UNPRIVILEGED_USER_ID, false);
        assertGetAllForVdsGroupCorrectResult(result);
    }

    private void assertGetAllForVdsGroupCorrectResult(List<VDS> result) {
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(result.iterator().next(), existingVds);

        for (VDS vds : result) {
            assertEquals(vds.getvds_group_id(), existingVds.getvds_group_id());
        }
    }

    /**
     * Asserts that the right collection of hosts is returned for a storage pool with hosts
     */
    @Test
    public void testGetAllForStoragePool() {
        List<VDS> result = dao.getAllForStoragePool(existingVds.getstorage_pool_id());
        assertGetAllForStoragePoolCorrectResult(result);
    }

    /**
     * Asserts that an empty collection of hosts is returned for a storage pool with no hosts
     */
    @Test
    public void testGetAllForStoragePoolNoVds() {
        List<VDS> result = dao.getAllForStoragePool(Guid.NewGuid());
        assertIncorrectGetResult(result);
    }

    /**
     * Asserts that the right collection of hosts is returned for a storage pool with hosts,
     * with a privileged user
     */
    @Test
    public void testGetAllForStoragePoolWithPermissions() {
        List<VDS> result = dao.getAllForStoragePool(existingVds.getstorage_pool_id(), PRIVILEGED_USER_ID, true);
        assertGetAllForStoragePoolCorrectResult(result);
    }

    /**
     * Asserts that the right collection of hosts is returned for a storage pool with hosts,
     * with an unprivileged user, but with the permissions mechanism disabled
     */
    @Test
    public void testGetAllForStoragePoolWithNoPermissionsFilteringDisabled() {
        List<VDS> result = dao.getAllForStoragePool(existingVds.getstorage_pool_id(), UNPRIVILEGED_USER_ID, false);
        assertGetAllForStoragePoolCorrectResult(result);
    }

    /**
     * Asserts that an empty collection of hosts is returned for a storage pool with hosts,
     * with an unprivileged user
     */
    @Test
    public void testGetAllForStoragePoolWithNoPermissions() {
        List<VDS> result = dao.getAllForStoragePool(existingVds.getstorage_pool_id(), UNPRIVILEGED_USER_ID, true);
        assertIncorrectGetResult(result);
    }

    private void assertGetAllForStoragePoolCorrectResult(List<VDS> result) {
        assertNotNull(result);
        assertFalse(result.isEmpty());

        for (VDS vds : result) {
            assertEquals("Wrong storage pool for VDS", existingVds.getstorage_pool_id(), vds.getstorage_pool_id());
        }
    }

    private void assertCorrectGetAllResult(List<VDS> result) {
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
