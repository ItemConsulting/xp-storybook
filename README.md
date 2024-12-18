# Storybook Server integration for Enonic XP

This application integrates with [Storybook Server renderer](https://www.npmjs.com/package/@storybook/server) and can
render [Freemarker templates](https://github.com/tineikt/xp-lib-freemarker/) or [Thymeleaf templates](https://github.com/enonic/lib-thymeleaf) with the args from Storybook.

[![](https://repo.itemtest.no/api/badge/latest/releases/no/item/xp-storybook)](https://repo.itemtest.no/#/releases/no/item/xp-storybook)


<img src="https://github.com/ItemConsulting/xp-storybook/raw/main/docs/icon.svg?sanitize=true" width="150">

> [!CAUTION]  
> This application should **never** be deployed in production! An attacker can use this application to render any content on your domain.

## Configuration

You need to create a configuration file: **XP_HOME/config/no.item.storybook.cfg** with the following content:

```ini
iAmNotFoolishEnoughToDeployThisInProduction=true
xpResourcesDirPath=/home/ubuntu/code/my-xp-project/src/main/resources
renderMode=freemarker
```

| Config key                                    | Value                                                                                                                                                                                  |
|-----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `iAmNotFoolishEnoughToDeployThisInProduction` | You will set this to `true` to indicate that you understand that this must **never be deployed** on a server open to the internet.                                                     |
| `xpResourcesDirPath`                          | The resources directory in your XP-project. This can also be a comma separated string with multiple resource directories (Freemarker only).                                            |
| `renderMode` (optional)                       | If the template language can not be determined by the file extension or `renderMode` query parameter, this fallback value will be used. Legal options are: `freemarker` or `thymeleaf` |

## Getting started

You will find information on how to set up your project in the [xp-storybook-utils documentation](https://github.com/ItemConsulting/xp-storybook-utils).

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
