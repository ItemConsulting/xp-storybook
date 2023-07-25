# Storybook Server integration for Enonic XP

<img src="https://github.com/ItemConsulting/xp-storybook/raw/main/docs/icon.svg?sanitize=true" width="150">

> **Warning** This application should **never** be deployed in production!
> An attacker can use this application to render any content on your domain.

## Configuration

You need to create a configuration file: **XP_HOME/config/no.item.storybook.cfg** with the following content:

```ini
iAmNotFoolishEnoughToDeployThisInProduction=true
```

## Deploying

### Building

To build he project run the following code

```bash
enonic project build
```

### Deploy locally

Deploy locally for testing purposes:

```bash
enonic project deploy
```
