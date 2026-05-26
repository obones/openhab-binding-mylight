# MyLight Binding

openHAB binding for [MyLight](https://www.mylight150.com/) electricity provider providing a bridge and associated components: smart battery,
power counter, water heater relay and virtual counters.

- [MyLight Binding](#mylight-binding)
  - [Supported Things](#supported-things)
    - [MyLight bridge](#mylight-bridge)
    - [Smart battery](#smart-battery)
    - [Power counter](#power-counter)
    - [Water heater relay](#water-heater-relay)
    - [Virtual counters](#virtual-counters)
  - [Discovery](#discovery)
  - [Console helper](#console-helper)
    - [List of bridges](#list-of-bridges)
    - [List of devices associated to a bridge](#list-of-devices-associated-to-a-bridge)
  - [Thing Configuration](#thing-configuration)
    - [MyLight bridge](#mylight-bridge-1)
    - [Smart battery](#smart-battery-1)
    - [Power counter](#power-counter-1)
    - [Water heater relay](#water-heater-relay-1)
    - [Virtual counters](#virtual-counters-1)
  - [Properties](#properties)
  - [Channels](#channels)
    - [MyLight bridge](#mylight-bridge-2)
    - [Smart battery](#smart-battery-2)
    - [Power counter](#power-counter-2)
    - [Water heater relay](#water-heater-relay-2)
    - [Virtual counters](#virtual-counters-2)

## Supported Things

There are five supported things.

### MyLight bridge

The bridge represents the connection to the MyLight website.
You must provide your login and password for the binding to retrieve values from the online console.

### Smart battery

The `smart-battery` thing provides charge level in kWh and percentage for the smart battery associated to the installation.
If created manually, it needs a valid Device Id to properly read the values.

### Power counter

The `power-counter` thing provides kW instantaneous power reading for the various sensors installed in the breaker panel.
Note that the values are usually small, and so it is recommended to use at least 4 digits to see some values.
If created manually, it needs a valid Device Id to properly read the values.

### Water heater relay

The `water-heater` thing is a power counter with an additional channel that allows controlling the on-off state of the associated relay.
It is usually labeled Water heater in the breaker panel.
If created manually, it needs a valid Device Id to properly read the values, one that is associated with a "Power actuator".

### Virtual counters

The `virtual-counters` thing provides power values aggregated by the provider, like Grid energy, Green energy...
If created manually, it needs a valid Device Id to properly read the values.

## Discovery

Once a bridge is properly configured and connected the discovery service will find all associated components and create a thing for each of them.
It is the easiest way to get the proper Device Id values for the given things.

## Console helper

If you don't want to use the discovery results, you will need to find the available Device Ids from the API result. While this can be done by analyzing the
website results in the developer pane of your favorite browser, this binding also provides a console helper that provides this information.
Once connected to the console, you can use the following commands:

### List of bridges

`mylight bridges`

This gives a list of installed bridges along with their UIDs that you will need for the second command, like this:

```console
openhab> mylight bridges
mylight:mylight:abcd12345e
```

### List of devices associated to a bridge

`mylight <bridgeUID> devices`

This gives a list of devices connected to the bridge, in the following format.

```console
openhab> mylight mylight:mylight:abcd12345e devices
*--------------------------------*---------------------------*---------------------------*--------------*
| Name                           | Device Id                 | Type                      | Has actuator |
*--------------------------------*---------------------------*---------------------------*--------------*
| Battery 1                      | 2DFOFOtzq4ttHav9_msb      | my_smart_battery          | false        |
| Virtual 1                      | rXEYcci1453tycya          | virtual                   | false        |
| Master                         | BD743101F4CF              | asoka_red_plug            | false        |
| Compteur                       | BD743201F4CF              | composite_device          | false        |
| Compteur 1                     | BD743101F4CF_RS_01        | water_heater              | false        |
| Compteur de consommation       | JpB45678fRG96ZZZ          | asoka_electric_counter    | false        |
| Compteur de production         | AAAATJDhZZZZZZZZ          | production_counter        | false        |
| Compteur de production 1       | 1111c1I001234567          | production_counter        | false        |
| Relais                         | BD743301F4CF              | water_heater              | true         |
*--------------------------------*---------------------------*---------------------------*--------------*
```

## Thing Configuration

### MyLight bridge

| Parameter       | Description                                                                                             |
|-----------------|---------------------------------------------------------------------------------------------------------|
| refreshInterval | Specifies the refresh interval (in minutes). Optional, the default value is 5, the minimum value is 1.  |
| email           | The email of your MyLight account.                                                                      |
| password        | The password associated to your email for the MyLight account                                           |
| baseURI         | The base URI to connect to. The default value is fine for connecting to the website.                    |

### Smart battery

| Parameter        | Description                                                         |
|------------------|---------------------------------------------------------------------|
| Device Id        | The device id for the device, as read from the API. **Mandatory**   |

### Power counter

| Parameter        | Description                                                         |
|------------------|---------------------------------------------------------------------|
| Device Id        | The device id for the device, as read from the API. **Mandatory**   |

### Water heater relay

| Parameter        | Description                                                         |
|------------------|---------------------------------------------------------------------|
| Device Id        | The device id for the device, as read from the API. **Mandatory**   |

### Virtual counters

| Parameter        | Description                                                         |
|------------------|---------------------------------------------------------------------|
| Device Id        | The device id for the device, as read from the API. **Mandatory**   |

## Properties

All things but the bridge offer a `last-updated` property that contains the last time the thing retrieved its values from the bridge.

This is a string in ISO 8601 format, such as: 2024-07-03T14:17:37Z

Additionally, the `smart-battery` thing has a `battery-capacity` property that gives you the kWh capacity of the SmartBattery associated to your account.

## Channels

### MyLight bridge

The bridge provides the following channels.

| Channel ID                 | Item Type            | Description |
|----------------------------|----------------------|-----------------------------------|
| last-updated               | DateTime             | Date and time when the bridge last triggered an update of all things connected to it |

### Smart battery

| Channel ID                     | Item Type            | Description |
|--------------------------------|----------------------|-----------------------------------|
| charge-level                   | Number:Dimensionless | The battery charge level in percent |
| charge-energy                  | Number:Energy        | The battery charge level in kWh |
| instantaneous-charge-energy    | Number:Energy        | The instantaneous charge energy, that is what is sent to the virtual battery |
| instantaneous-discharge-energy | Number:Energy        | The instantaneous discharge energy, that is what is received from the virtual battery |
| instantaneous-loss-energy      | Number:Energy        | The instantaneous loss energy, that is what is not stored in the battery because it is already full |

### Power counter

| Channel ID                     | Item Type            | Description |
|--------------------------------|----------------------|-----------------------------------|
| electric-power                 | Number:Power         | The currently measured power |

### Water heater relay

| Channel ID                     | Item Type            | Description |
|--------------------------------|----------------------|-----------------------------------|
| electric-power                 | Number:Power         | The currently measured power |
| on-off                         | Switch               | The on/off state of the associated relay |

### Virtual counters

| Channel ID                     | Item Type            | Description |
|--------------------------------|----------------------|-----------------------------------|
| produced-energy                | Number:Energy        | The produced energy, from the local system |
| electricity-meter-energy       | Number:Energy        | The energy measured at the electricity meter, can be negative is surplus energy is sent to the grid |
| total-energy                   | Number:Energy        | Total energy currently in use |
| green-energy                   | Number:Energy        | Locally produced energy currently in use |
| grid-energy                    | Number:Energy        | Energy currently in use taken from the grid |
| autonomy-rate                  | Number:Dimensionless | Autonomy rate |
| self-consumption               | Number:Dimensionless | Self consumption rate |
