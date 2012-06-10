package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.action.SetupNetworksParameters;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.NetworkUtils;

public class SetupNetworksHelper {
    private SetupNetworksParameters params;
    private Guid vdsGroupId;
    private List<VdcBllMessages> violations = new ArrayList<VdcBllMessages>();
    private Map<String, VdsNetworkInterface> existingIfaces;
    private Map<String, network> existingClusterNetworks;

    private List<network> modifiedNetworks = new ArrayList<network>();
    private List<String> unmanagedNetworks = new ArrayList<String>();
    private List<String> removedNetworks = new ArrayList<String>();
    private Map<String, VdsNetworkInterface> modifiedBonds = new HashMap<String, VdsNetworkInterface>();
    private List<VdsNetworkInterface> removedBonds = new ArrayList<VdsNetworkInterface>();

    /** All interface`s names that were processed by the helper. */
    private Set<String> ifaceNames = new HashSet<String>();

    /** Map of all bonds which were processed by the helper. Key = bond name, Value = list of slave NICs. */
    private Map<String, List<VdsNetworkInterface>> bonds = new HashMap<String, List<VdsNetworkInterface>>();

    /** All network`s names that are attached to some sort of interface. */
    private Set<String> attachedNetworksNames = new HashSet<String>();

    public SetupNetworksHelper(SetupNetworksParameters parameters, Guid vdsGroupId) {
        params = parameters;
        this.vdsGroupId = vdsGroupId;
    }

    /**
     * validate and extract data from the list of interfaces sent. The general flow is:
     * <ul>
     * <li>create mapping of existing the current topology - interfaces and logical networks.
     * <li>create maps for networks bonds and bonds-slaves.
     * <li>iterate over the interfaces and extract network/bond/slave info as we go.
     * <li>validate the extracted information by using the pre-build mappings of the current topology.
     * <li>store and encapsulate the extracted lists to later be fetched by the calling command.
     * <li>error messages are aggregated
     * </ul>
     * TODO add fail-fast to exist on the first validation error.
     *
     * @return List of violations encountered (if none, list is empty).
     */
    public List<VdcBllMessages> validate() {
        for (VdsNetworkInterface iface : params.getInterfaces()) {
            String name = iface.getName();
            if (addInterfaceToProcessedList(iface)) {
                if (isBond(iface)) {
                    extractBondIfModified(iface, name);
                } else {
                    if (StringUtils.isNotBlank(iface.getBondName())) {
                        extractBondSlave(iface);
                    }

                    // validate the nic exists on host
                    if (!getExistingIfaces().containsKey(NetworkUtils.StripVlan(name))) {
                        violations.add(VdcBllMessages.NETWORK_INTERFACE_NOT_EXISTS);
                    }
                }

                // validate and extract to network map
                if (violations.isEmpty() && StringUtils.isNotBlank(iface.getNetworkName())) {
                    extractNetwork(iface);
                }
            }
        }

        validateBondSlavesCount();
        detectSlaveChanges();
        extractRemovedNetworks();
        extractRemovedBonds();

        return violations;
    }

    /**
     * Add the given interface to the list of processed interfaces, failing if it already existed.
     *
     * @param iface
     *            The interface to add.
     * @return <code>true</code> if interface wasn't in the list and was added to it, otherwise <code>false</code>.
     */
    private boolean addInterfaceToProcessedList(VdsNetworkInterface iface) {
        if (ifaceNames.contains(iface.getName())) {
            if (isBond(iface)) {
                violations.add(VdcBllMessages.NETWORK_BOND_NAME_EXISTS);
            } else {
                violations.add(VdcBllMessages.NETWORK_INTERFACE_NAME_ALREADY_IN_USE);
            }

            return false;
        }

        ifaceNames.add(iface.getName());
        return true;
    }

    /**
     * Detect a bond that it's slaves have changed, to add to the modified bonds list.
     */
    private void detectSlaveChanges() {
        for (Map.Entry<String, List<VdsNetworkInterface>> bondEntry : bonds.entrySet()) {
            String bondName = bondEntry.getKey();
            if (!modifiedBonds.containsKey(bondName)) {
                for (VdsNetworkInterface bondSlave : bondEntry.getValue()) {
                    if (interfaceWasModified(bondSlave)) {
                        modifiedBonds.put(bondName, getExistingIfaces().get(bondName));
                    }
                }
            }
        }
    }

    private Map<String, network> getExistingClusterNetworks() {
        if (existingClusterNetworks == null) {
            existingClusterNetworks = Entities.entitiesByName(
                    getDbFacade().getNetworkDAO().getAllForCluster(vdsGroupId));
        }

        return existingClusterNetworks;
    }

    private Map<String, VdsNetworkInterface> getExistingIfaces() {
        if (existingIfaces == null) {
            existingIfaces = Entities.entitiesByName(
                    getDbFacade().getInterfaceDAO().getAllInterfacesForVds(params.getVdsId()));
        }

        return existingIfaces;
    }

    protected DbFacade getDbFacade() {
        return DbFacade.getInstance();
    }

    /**
     * extracting a network is done by matching the desired network name with the network details from db on
     * clusterNetworksMap. The desired network is just a key and actual network configuration is taken from the db
     * entity.
     *
     * @param iface
     *            current iterated interface
     */
    private void extractNetwork(VdsNetworkInterface iface) {
        String networkName = iface.getNetworkName();

        // prevent attaching 2 interfaces to 1 network
        if (attachedNetworksNames.contains(networkName)) {
            violations.add(VdcBllMessages.NETWROK_ALREADY_ATTACHED_TO_INTERFACE);
        } else {
            attachedNetworksNames.add(networkName);

            // check if network exists on cluster
            if (getExistingClusterNetworks().containsKey(networkName)) {
                if (interfaceWasModified(iface)) {
                    modifiedNetworks.add(getExistingClusterNetworks().get(networkName));
                }

                // Interface must exist, it was checked before and we can't reach here if it does'nt exist already.
            } else if (networkName.equals(getExistingIfaces().get(iface.getName()).getNetworkName())) {
                unmanagedNetworks.add(networkName);
            } else {
                violations.add(VdcBllMessages.NETWORK_NOT_EXISTS_IN_CURRENT_CLUSTER);
            }
        }
    }

    /**
     * Check if the given interface was modified (or added).
     *
     * @param iface
     *            The interface to check.
     * @return <code>true</code> if the interface was changed, or is a new one. <code>false</code> if it existed and
     *         didn't change.
     */
    private boolean interfaceWasModified(VdsNetworkInterface iface) {
        return !iface.equals(getExistingIfaces().get(iface.getName()));
    }

    private boolean isBond(VdsNetworkInterface iface) {
        return Boolean.TRUE.equals(iface.getBonded());
    }

    /**
     * build mapping of the bond name - > list of slaves. slaves are interfaces with a pointer to the master bond by
     * bondName.
     *
     * @param iface
     */
    private void extractBondSlave(VdsNetworkInterface iface) {
        List<VdsNetworkInterface> slaves = bonds.get(iface.getBondName());
        if (slaves == null) {
            slaves = new ArrayList<VdsNetworkInterface>();
            bonds.put(iface.getBondName(), slaves);
        }

        slaves.add(iface);
    }

    /**
     * Extract the bond to the modified bonds list if it was added or the bond interface config has changed.
     *
     * @param iface
     *            The interface of the bond.
     * @param bondName
     *            The bond name.
     */
    private void extractBondIfModified(VdsNetworkInterface iface, String bondName) {
        if (!bonds.containsKey(bondName)) {
            bonds.put(bondName, new ArrayList<VdsNetworkInterface>());
        }

        if (interfaceWasModified(iface)) {
            modifiedBonds.put(bondName, iface);
        }
    }

    /**
     * Extract the bonds to be removed. If a bond was attached to slaves but it's not attached to anything then it
     * should be removed. Otherwise, no point in removing it: Either it is still a bond, or it isn't attached to any
     * slaves either way so no need to touch it.
     */
    private void extractRemovedBonds() {
        for (VdsNetworkInterface iface : getExistingIfaces().values()) {
            String bondName = iface.getBondName();
            if (StringUtils.isNotBlank(bondName) && !bonds.containsKey(bondName)) {
                removedBonds.add(getExistingIfaces().get(bondName));
            }
        }
    }

    private boolean validateBondSlavesCount() {
        boolean returnValue = true;
        for (Map.Entry<String, List<VdsNetworkInterface>> bondEntry : bonds.entrySet()) {
            if (bondEntry.getValue().size() < 2) {
                returnValue = false;
                violations.add(VdcBllMessages.NETWORK_BOND_PARAMETERS_INVALID);
            }
        }

        return returnValue;
    }

    /**
     * Calculate the networks that should be removed - If the network was attached to a NIC and is no longer attached to
     * it, then it will be removed.
     */
    private void extractRemovedNetworks() {
        for (VdsNetworkInterface iface : getExistingIfaces().values()) {
            String net = iface.getNetworkName();
            if (StringUtils.isNotBlank(net) && !attachedNetworksNames.contains(net)) {
                removedNetworks.add(net);
            }
        }
    }

    public List<network> getNetworks() {
        return modifiedNetworks;
    }

    public List<String> getRemoveNetworks() {
        return removedNetworks;
    }

    public List<VdsNetworkInterface> getBonds() {
        return new ArrayList<VdsNetworkInterface>(modifiedBonds.values());
    }

    public List<VdsNetworkInterface> getRemovedBonds() {
        return removedBonds;
    }
}
