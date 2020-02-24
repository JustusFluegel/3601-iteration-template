import {AppPage} from './app.po';

describe('angular-spark-lab', () => {
  let page: AppPage;

  beforeEach(() => {
    page = new AppPage();
  });

  it('Should load', () => {
    page.navigateTo();
  });

  it('Should have the correct title', () => {
    page.navigateTo();
    expect(page.getAppTitle()).toEqual('CSCI 3601 Lab 4');
  });

  it('Should open the sidenav', () => {
    page.navigateTo();

    // Before clicking on the button, the sidenav should be hidden
    expect(page.getSidenav().isDisplayed()).toBe(false);

    page.openSideNav().then(() => {
      // After clicking the button the sidenav should be displayed
      expect(page.getSidenav().isDisplayed()).toBe(true);
    });
  });
});
