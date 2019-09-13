# IMPORTANT NOTICE
This repository is no longer current. Please see [SatokenCore](https://github.com/Septem151/SatokenCore) repository for a more up-to-date and maintained Java blockchain.

This master branch is currently not complete (and never will be). "blockchain" folder that is created after running must be deleted between subsequent runs of the program, as loading/saving is currently broken.

# MockBlockchain
A Mock Blockchain created in Java using actual Bitcoin addresses. Transactions on the blockchain are able to process Many-to-One payments, combining multiple Unspent Transaction Outputs from multiple addresses in a wallet, into a singular Unspent Transaction Output to an address specified. BIP32 & BIP39 provides the ability for restoration of wallets and their respective Private Keys from a 12-word mnemonic phrase.


## How to use
### From Release
Download the latest release from https://github.com/Septem151/MockBlockchain/releases and run the Launch.bat file, or by using the command
`$ java -jar SatokenCore.jar` within the directory saved.

### From Source
Download the source code as a .ZIP file from this repository and extract, or clone into a new directory:

```$ git clone https://github.com/Septem151/MockBlockchain.git```

Navigate to the ```dist``` folder and run Launch.bat , or open a Command Prompt and cd to the dist folder and use the command:

```$ java -jar SatokenCore.jar```

## Contributions
If you would like to contribute to this project, please fork the repository. Below is a list of tasks that need to be completed:
- [ ] Fix Saving/Loading data on-close, as recent BIP32/BIP39 implementations broke this feature
- [ ] Restore Wallet From Seed option when using `newaccount` command
- [ ] Add `newaddress` command to generate a fresh address for the current wallet open
- [ ] Edit Driver.loadWordList() function to check if "Word List.txt" exists before calling Bitcoin's GitHub URL

#### This program is intended for educational purposes only.
